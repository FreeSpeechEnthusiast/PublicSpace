/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD+Patents license found in the
 * LICENSE file in the root directory of this source tree.
 */

// -*- C++ -*-

// This is heavily modified version of https://github.com/facebookresearch/faiss/blob/main/faiss/python/swigfaiss.swig

%pragma(java) jniclassimports = %{import com.twitter.ann.faiss.NativeUtils;%}

%pragma(java) jniclasscode = %{
  static {
    try {
      if (NativeUtils.getOperatingSystemType() == NativeUtils.OSType.MacOS) {
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/swigfaiss.dylib");
      } else {
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/libstdc++.so.6");
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/libgcc_s.so.1");
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/libgomp.so.1");
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/libquadmath.so.0");
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/libgfortran.so.5");
        NativeUtils.loadLibraryFromJar("/com/twitter/ann/faiss/swig/resources/swigfaiss.so");
      }
    } catch (Exception e) {
      System.err.println("Native code library failed to load. \n" + e);
      System.exit(1);
    }
  }
%}

#ifdef SWIGJAVA
%include "arrays_java.i"
%include "carrays.i"
%array_class(int, intArray);
%array_class(float, floatArray);
%array_class(long long, longArray);
%array_class(double, doubleArray);
#endif

#ifdef GPU_WRAPPER
%module swigfaiss_gpu;
#else
%module swigfaiss;
#endif

// fbode SWIG fails on warnings, so make them non fatal
#pragma SWIG nowarn=321
#pragma SWIG nowarn=403
#pragma SWIG nowarn=325
#pragma SWIG nowarn=389
#pragma SWIG nowarn=341

typedef unsigned long uint64_t;
typedef uint64_t size_t;
typedef int int32_t;
typedef unsigned char uint8_t;

#define __restrict


/*******************************************************************
 * Copied verbatim to wrapper. Contains the C++-visible includes, and
 * the language includes for their respective matrix libraries.
 *******************************************************************/

%{

#include <stdint.h>
#include <omp.h>
#include <regex>

#include "MetricType.h"

#include "IndexFlat.h"
#include "VectorTransform.h"
#include "IndexLSH.h"
#include "IndexPQ.h"
#include "IndexIVF.h"
#include "IndexIVFPQ.h"
#include "IndexIVFPQR.h"
#include "IndexIVFFlat.h"
#include "IndexScalarQuantizer.h"
#include "impl/HNSW.h"
#include "IndexHNSW.h"
#include "MetaIndexes.h"
#include "IndexShards.h"
#include "impl/FaissAssert.h"

#include "IndexPreTransform.h"
#include "Index2Layer.h"
#include "IndexBinaryFlat.h"
#include "IndexBinaryIVF.h"
#include "IndexBinaryFromFloat.h"
#include "IndexBinaryHNSW.h"

#include "index_io.h"

#include "IVFlib.h"
#include "utils/utils.h"
#include "utils/Heap.h"
#include "impl/AuxIndexStructures.h"

#include "invlists/OnDiskInvertedLists.h"

#include "Clustering.h"

#include "utils/hamming.h"
#include "utils/partitioning.h"

#include "AutoTune.h"
#include "index_factory.h"
#include "IndexRefine.h"

using namespace faiss;
using idx_t = Index::idx_t;

%}

%typemap(in) int64_t {
  $1 = $input;
}
%typemap(out) int64_t {
  $result = $1;
}
%typemap(jtype) int64_t "long"
%typemap(jstype) int64_t "long"
%typemap(javain) int64_t "$javainput"
%typemap(javaout) int64_t {
    return $jnicall;
}
// %typemap(jni) int64_t* "longjni"
// %typemap(jtype) int64_t* "longjtype"
%typemap(jstype) int64_t* "LongVector"
%typemap(javain) int64_t* "SWIGTYPE_p_long_long.getCPtr($javainput.data())"
%typemap(javaout) int64_t* {
    return new LongVector($jnicall, false);
}

%ignore faiss::InterruptCallback::instance;
%ignore faiss::InterruptCallback::lock;
%ignore faiss::RangeSearchResult::RangeSearchResult;
%ignore faiss::RangeSearchPartialResult::RangeSearchPartialResult;
%ignore faiss::RangeSearchPartialResult::finalize;
%ignore faiss::IndexSplitVectors::IndexSplitVectors;

// produces an error on the Mac
%ignore faiss::hamming;
#define FAISS_API

/*******************************************************************
 * Types of vectors we want to manipulate at the scripting language
 * level.
 *******************************************************************/

// simplified interface for vector
namespace std {

    template<class T>
    class vector {
    public:
        vector();
        void push_back(T);
        void clear();
        T * data();
        size_t size();
        T at (size_t n) const;
        void resize (size_t n);
        void reserve (size_t n);
        void swap (vector<T> & other);
    };
};



%template(FloatVector) std::vector<float>;
%template(DoubleVector) std::vector<double>;
%template(ByteVector) std::vector<uint8_t>;
%template(CharVector) std::vector<char>;
// NOTE(hoss): Using unsigned long instead of uint64_t because OSX defines
//   uint64_t as unsigned long long, which SWIG is not aware of.
%template(Uint64Vector) std::vector<unsigned long>;
%template(LongVector) std::vector<long long>;
%template(IntVector) std::vector<int>;
%template(VectorTransformVector) std::vector<faiss::VectorTransform*>;
%template(OperatingPointVector) std::vector<faiss::OperatingPoint>;
%template(InvertedListsPtrVector) std::vector<faiss::InvertedLists*>;
%template(FloatVectorVector) std::vector<std::vector<float> >;
%template(ByteVectorVector) std::vector<std::vector<unsigned char> >;
%template(LongVectorVector) std::vector<std::vector<long> >;



#ifdef GPU_WRAPPER
%template(GpuResourcesVector) std::vector<faiss::gpu::GpuResources*>;
#endif

%include <std_string.i>

%include <exception.i>
%exception {
    try {
        $action
    } catch (const std::regex_error& ex) {
        SWIG_exception(SWIG_SystemError, (std::string("std::regex_error has been thrown: '") + ex.what() + std::string("' code: ") + std::to_string(ex.code())).c_str());
    } catch (const std::exception& ex) {
        SWIG_exception(SWIG_SystemError, (std::string("Exception has been thrown: ") + ex.what()).c_str());
    }
}

/*******************************************************************
 * Parse headers
 *******************************************************************/
%ignore *::cmp;

%include "utils/Heap.h"
%include "utils/hamming.h"

int get_num_gpus();

#ifdef GPU_WRAPPER

%{

#include "gpu/StandardGpuResources.h"
#include "gpu/GpuIndicesOptions.h"
#include "gpu/GpuClonerOptions.h"
#include "gpu/utils/MemorySpace.h"
#include "gpu/GpuIndex.h"
#include "gpu/GpuIndexFlat.h"
#include "gpu/GpuIndexIVF.h"
#include "gpu/GpuIndexIVFPQ.h"
#include "gpu/GpuIndexIVFFlat.h"
#include "gpu/GpuIndexBinaryFlat.h"
#include "gpu/IndexProxy.h"
#include "gpu/GpuAutoTune.h"
#include "gpu/GpuDistance.h"

int get_num_gpus()
{
    return faiss::gpu::getNumDevices();
}

%}

// causes weird wrapper bug
%ignore *::getMemoryManager;
%ignore *::getMemoryManagerCurrentDevice;

%include "gpu/GpuResources.h"
%include "gpu/StandardGpuResources.h"

#else

%{
int get_num_gpus()
{
    return 0;
}
%}


#endif


%include "MetricType.h"
%include "utils/utils.h"

%include "Index.h"
%include "Clustering.h"

%ignore faiss::ProductQuantizer::get_centroids(size_t,size_t) const;
%ignore faiss::IndexShardsTemplate::IndexShardsTemplate(long d, bool threaded);
%ignore faiss::IndexShardsTemplate::IndexShardsTemplate(idx_t d, bool threaded = false, bool successive_ids = true);

%include "impl/ProductQuantizer.h"

%include "VectorTransform.h"
%include "IndexFlatCodes.h"
%include "IndexFlat.h"
%include "IndexLSH.h"
%include "impl/PolysemousTraining.h"
%include "IndexPQ.h"
%include "invlists/InvertedLists.h"
%ignore InvertedListScanner;
%ignore BinaryInvertedListScanner;
%include "IndexIVF.h"
// NOTE(hoss): SWIG (wrongly) believes the overloaded const version shadows the
//   non-const one.
%warnfilter(509) extract_index_ivf;
%include "utils/hamming-inl.h"
%include "IVFlib.h"
%include "IndexScalarQuantizer.h"
%include "impl/HNSW.h"
%include "IndexHNSW.h"
%include "IndexIVFFlat.h"
%include "invlists/OnDiskInvertedLists.h"

%ignore faiss::IndexIVFPQ::alloc_type;
%include "IndexIVFPQ.h"

%include "IndexBinary.h"
%include "Index2Layer.h"
%include "IndexBinaryFlat.h"
%include "IndexBinaryIVF.h"
%include "IndexBinaryFromFloat.h"
%include "IndexBinaryHNSW.h"
%include "IndexRefine.h"

%include "MetaIndexes.h"
%template(IndexIDMap) faiss::IndexIDMapTemplate<faiss::Index>;

%include  <faiss/IndexShards.h>
%template(IndexShards) faiss::IndexShardsTemplate<faiss::Index>;
#ifdef GPU_WRAPPER

// quiet SWIG warnings
%ignore faiss::gpu::GpuIndexIVF::GpuIndexIVF;
%ignore faiss::gpu::IndexProxy::at(int) const;

%include "gpu/GpuIndicesOptions.h"
%include "gpu/GpuClonerOptions.h"
%include "gpu/utils/MemorySpace.h"
%include "gpu/GpuIndex.h"
%include "gpu/GpuIndexFlat.h"
%include "gpu/GpuIndexIVF.h"
%include "gpu/GpuIndexIVFPQ.h"
%include "gpu/GpuIndexIVFFlat.h"
%include "gpu/GpuIndexBinaryFlat.h"
%include "gpu/IndexProxy.h"
%include "gpu/GpuDistance.h"

#endif

%ignore faiss::Cloner;

%extend faiss::Index {
  faiss::IndexIVF *toIVF() {
    return dynamic_cast<faiss::IndexIVF *>($self);
  }
}

/*******************************************************************
 * downcast return of some functions so that the sub-class is used
 * instead of the generic upper-class.
 *******************************************************************/
#ifdef SWIGJAVA

%define DOWNCAST(subclass)
    if (dynamic_cast<faiss::subclass *> ($1)) {
        faiss::subclass *instance_ptr = (faiss::subclass *)$1;
        $result = (jlong)instance_ptr;
    } else
%enddef

%define DOWNCAST_GPU(subclass)
    if (dynamic_cast<faiss::gpu::subclass *> ($1)) {
        faiss::subclass *instance_ptr = (faiss::subclass *)$1;
        $result = (jlong)instance_ptr;
    } else
%enddef

#endif

%newobject read_index;
%newobject read_index_binary;
%newobject read_VectorTransform;
%newobject read_ProductQuantizer;
%newobject clone_index;
%newobject clone_VectorTransform;

// Subclasses should appear before their parent
%typemap(out) faiss::Index * {
    DOWNCAST ( IndexIDMap )
    DOWNCAST ( IndexShards )
    DOWNCAST ( IndexIVFPQR )
    DOWNCAST ( IndexIVFPQ )
    DOWNCAST ( IndexIVFScalarQuantizer )
    DOWNCAST ( IndexIVFFlatDedup )
    DOWNCAST ( IndexIVFFlat )
    DOWNCAST ( IndexIVF )
    DOWNCAST ( IndexFlat )
    DOWNCAST ( IndexPQ )
    DOWNCAST ( IndexScalarQuantizer )
    DOWNCAST ( IndexLSH )
    DOWNCAST ( IndexPreTransform )
    DOWNCAST ( MultiIndexQuantizer )
    DOWNCAST ( IndexHNSWFlat )
    DOWNCAST ( IndexHNSWPQ )
    DOWNCAST ( IndexHNSWSQ )
    DOWNCAST ( IndexHNSW2Level )
    DOWNCAST ( Index2Layer )
#ifdef GPU_WRAPPER
    DOWNCAST_GPU ( IndexProxy )
    DOWNCAST_GPU ( GpuIndexIVFPQ )
    DOWNCAST_GPU ( GpuIndexIVFFlat )
    DOWNCAST_GPU ( GpuIndexFlat )
#endif
    // default for non-recognized classes
    DOWNCAST ( Index )
    if ($1 == NULL)
    {
#ifdef SWIGJAVA
        $result = 0;
#endif
        // Lua does not need a push for nil
    } else {
        assert(false);
    }
}

%typemap(out) faiss::IndexBinary * {
    DOWNCAST ( IndexBinaryIVF )
    DOWNCAST ( IndexBinaryFlat )
    DOWNCAST ( IndexBinaryFromFloat )
    DOWNCAST ( IndexBinaryHNSW )
#ifdef GPU_WRAPPER
    DOWNCAST_GPU ( GpuIndexBinaryFlat )
#endif
    // default for non-recognized classes
    DOWNCAST ( IndexBinary )
    if ($1 == NULL)
    {
#ifdef SWIGJAVA
        $result = 0;
#endif
        // Lua does not need a push for nil
    } else {
        assert(false);
    }
}

%typemap(out) faiss::VectorTransform * {
    DOWNCAST (RemapDimensionsTransform)
    DOWNCAST (OPQMatrix)
    DOWNCAST (PCAMatrix)
    DOWNCAST (RandomRotationMatrix)
    DOWNCAST (LinearTransform)
    DOWNCAST (NormalizationTransform)
    DOWNCAST (VectorTransform)
    {
        assert(false);
    }
}

// just to downcast pointers that come from elsewhere (eg. direct
// access to object fields)
%inline %{
faiss::Index * downcast_index (faiss::Index *index)
{
    return index;
}
faiss::VectorTransform * downcast_VectorTransform (faiss::VectorTransform *vt)
{
    return vt;
}
faiss::IndexBinary * downcast_IndexBinary (faiss::IndexBinary *index)
{
    return index;
}
%}

%inline %{
faiss::Index * upcast_IndexShards (faiss::IndexShardsTemplate<faiss::Index> *index)
{
    return index;
}
%}


%include "index_io.h"

%newobject index_factory;
%newobject index_binary_factory;

%include "AutoTune.h"
%include  <faiss/index_factory.h>


#ifdef GPU_WRAPPER

%newobject index_gpu_to_cpu;
%newobject index_cpu_to_gpu;
%newobject index_cpu_to_gpu_multiple;

%include "gpu/GpuAutoTune.h"

#endif


/*******************************************************************
 * How should the template objects appear in the scripting language?
 *******************************************************************/

// answer: the same as the C++ typedefs, but we still have to redefine them

%include "utils/ordered_key_value.h"
%include "utils/AlignedTable.h"
%include "utils/partitioning.h"

%template() faiss::CMin<float, int64_t>;
%template() faiss::CMin<int, int64_t>;
%template() faiss::CMax<float, int64_t>;
%template() faiss::CMax<int, int64_t>;

%template(float_minheap_array_t) faiss::HeapArray<faiss::CMin<float, int64_t> >;
%template(int_minheap_array_t) faiss::HeapArray<faiss::CMin<int, int64_t> >;

%template(float_maxheap_array_t) faiss::HeapArray<faiss::CMax<float, int64_t> >;
%template(int_maxheap_array_t) faiss::HeapArray<faiss::CMax<int, int64_t> >;

%template(CMin_float_partition_fuzzy)
    faiss::partition_fuzzy<faiss::CMin<float, int64_t> >;
%template(CMax_float_partition_fuzzy)
    faiss::partition_fuzzy<faiss::CMax<float, int64_t> >;

%template(AlignedTableUint8) faiss::AlignedTable<uint8_t>;
%template(AlignedTableUint16) faiss::AlignedTable<uint16_t>;
%template(AlignedTableFloat32) faiss::AlignedTable<float>;

%inline %{

// SWIG seems to have has some trouble resolving the template type here, so
// declare explicitly
uint16_t CMax_uint16_partition_fuzzy(
        uint16_t *vals, int64_t *ids, size_t n,
        size_t q_min, size_t q_max, size_t * q_out)
{
    return faiss::partition_fuzzy<faiss::CMax<unsigned short, int64_t> >(
        vals, ids, n, q_min, q_max, q_out);
}

uint16_t CMin_uint16_partition_fuzzy(
        uint16_t *vals, int64_t *ids, size_t n,
        size_t q_min, size_t q_max, size_t * q_out)
{
    return faiss::partition_fuzzy<faiss::CMin<unsigned short, int64_t> >(
        vals, ids, n, q_min, q_max, q_out);
}

// and overload with the int32 version

uint16_t CMax_uint16_partition_fuzzy(
        uint16_t *vals, int *ids, size_t n,
        size_t q_min, size_t q_max, size_t * q_out)
{
    return faiss::partition_fuzzy<faiss::CMax<unsigned short, int> >(
        vals, ids, n, q_min, q_max, q_out);
}

uint16_t CMin_uint16_partition_fuzzy(
        uint16_t *vals, int *ids, size_t n,
        size_t q_min, size_t q_max, size_t * q_out)
{
    return faiss::partition_fuzzy<faiss::CMin<unsigned short, int> >(
        vals, ids, n, q_min, q_max, q_out);
}

%}

/*******************************************************************
 * Expose a few basic functions
 *******************************************************************/


void omp_set_num_threads (int num_threads);
int omp_get_max_threads ();
void *memcpy(void *dest, const void *src, size_t n);


/*******************************************************************
 * For Faiss/Pytorch interop via pointers encoded as longs
 *******************************************************************/

%inline %{
float * cast_integer_to_float_ptr (long x) {
    return (float*)x;
}

long * cast_integer_to_long_ptr (long x) {
    return (long*)x;
}

int * cast_integer_to_int_ptr (long x) {
    return (int*)x;
}

%}



/*******************************************************************
 * Range search interface
 *******************************************************************/

%ignore faiss::BufferList::Buffer;
%ignore faiss::RangeSearchPartialResult::QueryResult;
%ignore faiss::IDSelectorBatch::set;
%ignore faiss::IDSelectorBatch::bloom;

%include "impl/AuxIndexStructures.h"

%{
// may be useful for lua code launched in background from shell

#include <signal.h>
void ignore_SIGTTIN() {
    signal(SIGTTIN, SIG_IGN);
}
%}

void ignore_SIGTTIN();


%inline %{

// numpy misses a hash table implementation, hence this class. It
// represents not found values as -1 like in the Index implementation

struct MapLong2Long {
    std::unordered_map<long, long> map;

    void add(size_t n, const long *keys, const long *vals) {
        map.reserve(map.size() + n);
        for (size_t i = 0; i < n; i++) {
            map[keys[i]] = vals[i];
        }
    }

    long search(long key) {
        if (map.count(key) == 0) {
            return -1;
        } else {
            return map[key];
        }
    }

    void search_multiple(size_t n, const long *keys, long * vals) {
        for (size_t i = 0; i < n; i++) {
            vals[i] = search(keys[i]);
        }
    }
};

%}

// End of file...
