package com.twitter.auth.pasetoheaders.encryption;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import dev.paseto.jpaseto.io.jackson.JacksonDeserializer;
import dev.paseto.jpaseto.io.jackson.JacksonSerializer;
import dev.paseto.jpaseto.lang.DateFormats;

public final class ClaimMapping {
  private ObjectMapper customMapper;
  private JacksonDeserializer jacksonDeserializer;
  private JacksonSerializer jacksonSerializer;
  private Map<String, Class> claimTypeMap = new HashMap<>();

  public JacksonDeserializer getDeserializer() {
    return jacksonDeserializer;
  }

  public JacksonSerializer getSerializer() {
    return jacksonSerializer;
  }

  public ClaimMapping() {
    customMapper = new ObjectMapper();
    customMapper.registerModule(new Jdk8Module());
    customMapper.registerModule(new JavaTimeModule());
    customMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // custom time formatting replace 'Z' with +00:00
    customMapper.registerModule(new SimpleModule()
        .addSerializer(Instant.class, new NonZInstantSerializer())
    );
    customMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    customMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    jacksonDeserializer = new JacksonDeserializer(customMapper);
    jacksonSerializer = new JacksonSerializer(customMapper);
  }

  /**
   * Registers association between claim and POJO class, for proper serialization
   *
   * @param claim
   * @param objectClass
   */
  public void registerClaimMapping(String claim, Class objectClass) {
    claimTypeMap.put(claim, objectClass);
    SimpleModule simpleModule = new SimpleModule(objectClass.getSimpleName() + "Module");
    simpleModule.addSerializer(new ClaimSerializer());
    simpleModule.addDeserializer(Object.class, new MappedTypeDeserializer(
        Collections.unmodifiableMap(claimTypeMap)));
    customMapper.registerModule(simpleModule);
  }

  public Class getClaimType(String claim) {
    return claimTypeMap.get(claim);
  }

  /**
   * Important! Custom serialization allows to properly append type information to our claims
   */
  public class ClaimSerializer extends StdSerializer<Map> {
    protected ClaimSerializer() {
      super(Map.class);
    }
    @Override
    public void serialize(Map map,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider)
        throws IOException {
      jsonGenerator.writeStartObject();
      for (Object key : map.keySet()) {
        jsonGenerator.writeObjectField((String) key, map.get(key));
      }
      jsonGenerator.writeEndObject();
    }
  }

  private static final class NonZInstantSerializer extends InstantSerializer {
    private static final long serialVersionUID = 1L;

    private NonZInstantSerializer() {
      super(InstantSerializer.INSTANCE, true, DateFormats.ISO_OFFSET_DATE_TIME);
    }
  }

  /**
   * A Jackson {@link com.fasterxml.jackson.databind.JsonDeserializer JsonDeserializer},
   * that will convert claim
   * values to types based on {@code claimTypeMap}.
   */
  private static final class MappedTypeDeserializer extends UntypedObjectDeserializer {
    private static final long serialVersionUID = 1L;
    private final Map<String, Class> claimTypeMap;

    private MappedTypeDeserializer(Map<String, Class> claimTypeMap) {
      super(null, null);
      this.claimTypeMap = claimTypeMap;
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      // check if the current claim key is mapped, if so traverse it's value
      if (claimTypeMap != null && claimTypeMap.containsKey(parser.currentName())) {
        Class<?> type = claimTypeMap.get(parser.currentName());
        return parser.readValueAsTree().traverse(parser.getCodec()).readValueAs(type);
      }
      // otherwise default to super
      return super.deserialize(parser, context);
    }
  }

}
