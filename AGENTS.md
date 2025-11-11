# Agent Instructions for CampusCoffee Project

## Build Commands
- **Full build**: `mvn clean install`
- **Quiet build**: `mvn clean install -q`
- **Start app**: `cd application && mvn spring-boot:run -Dspring-boot.run.profiles=dev`

## Test Commands
- **All tests**: `mvn test`
- **Single class**: `mvn test -Dtest=ClassName`
- **Single method**: `mvn test -Dtest=ClassName#testMethodName`
- **System tests**: `mvn test -pl application -Dtest="*SystemTests"`

## Lint/Quality
- **Static analysis**: `mvn pmd:check pmd:cpd-check`
- **Reports**: `mvn site`

## Code Style Guidelines
**Tech Stack**: Java 21, Spring Boot 3.5.7, Maven 3.9, Hexagonal architecture

**Dependencies**: Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@NonNull`), MapStruct, JSpecify, Records

**Naming**: PascalCase classes, camelCase methods/vars, UPPER_SNAKE_CASE constants, lowercase packages

**Imports**: Spring → Lombok → Project (domain→data→api→application) → Java stdlib → Third-party

**Patterns**: Constructor injection, `@Slf4j` logging, custom domain exceptions, AssertJ tests, record builders

**Error Handling**: Domain exceptions in `domain.exceptions`, global handler in API layer, proper HTTP codes

**Structure**: domain/ (business logic), data/ (persistence), api/ (controllers/DTOs), application/ (config/tests)</content>
<parameter name="filePath">/home/alex/Documents/Uni/ISE/ise25-26_assignment04/AGENTS.md