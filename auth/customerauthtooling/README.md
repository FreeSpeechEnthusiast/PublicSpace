CustomerAuth Tooling Service
=============

## Development (IDE setup)
- fastpass create --name customerauthtooling auth/customerauthtooling::
- fastpass open --intellij customerauthtooling

## Local Service
- Run a local service: `./auth/customerauthtooling/scripts/runlocal.sh`
- View the server admin page: `$ open http://0.0.0.0:31382/admin`

## Debug
- Run a local service: `./auth/customerauthtooling/scripts/runlocal.sh -debug`
- Connect debugger using "Remote JVM Debug" on port 5009

## CLI
- Run a local service: `./auth/customerauthtooling/scripts/cli.sh`
- [Optional] Connect debugger using "Remote JVM Debug" on port 5005
- [Optional] Use local service:
`./auth/customerauthtooling/scripts/cli.sh --dtab="/s/customerauthtooling/customerauthtooling=>/$/inet/127.0.0.1/31908"`
- Use help command to browse possible options
```bash
CustomerAuth> help
```
  
## Unit & Feature Tests
TBA

## Local Load Test
TBA

## Remote Load Test
TBA

## FAQ
TBA
