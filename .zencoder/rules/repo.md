---
description: Repository Information Overview
alwaysApply: true
---

# GeoNetwork Information

## Summary
GeoNetwork opensource is a standards-based, free and open-source catalog application to manage spatially referenced resources through the web. It provides powerful metadata editing and search functions, an interactive web map viewer, and supports various metadata standards.

## Structure
- **core**: Core functionality of GeoNetwork
- **web**: Web module containing the main application
- **web-ui**: AngularJS-based user interface
- **schemas**: Metadata schemas (ISO19139, Dublin Core, etc.)
- **services**: API services
- **harvesters**: Metadata harvesting components
- **es**: Elasticsearch integration
- **docker**: Development Docker environments
- **docs**: Documentation and manuals

## Language & Runtime
**Language**: Java
**Version**: Java 11
**Build System**: Maven (3.8.3+)
**Package Manager**: Maven

## Dependencies
**Main Dependencies**:
- Spring Framework
- Elasticsearch (for indexing)
- AngularJS (frontend)
- Bootstrap (UI components)
- Jetty (embedded server)

## Build & Installation
```bash
# Build the complete application
mvn clean install

# Run Elasticsearch (required)
cd es
mvn install -Pes-download
mvn exec:exec -Des-start

# Run the application with embedded Jetty
cd web
mvn jetty:run -Penv-dev
```

## Docker
**Configuration**: Development Docker environments available in the `docker` directory
```bash
# See docker directory for development environments
cd docker/gn-postgres
```

## Testing
**Framework**: JUnit
**Test Types**: Unit tests and Integration tests
**Run Command**:
```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify -Pit
```

## Frontend Development
**Framework**: AngularJS
**Build Tool**: Wro4j
**Localization**: Transifex integration
```bash
# Format code
cd web-ui
# Download translations
./download-from-transifex.sh
```