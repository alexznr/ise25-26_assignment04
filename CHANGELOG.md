# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Changed

- Fix broken test case in `PosSystemTests` (assignment 3).
- Extend GitHub Actions triggers to include pushes to feature branches (assignment 3).
- Add new `POST` endpoint `/api/pos/import/osm/{nodeId}` that allows API users to import a `POS` based on an OpenStreetMap node.
- Extend `PosService` interface by adding a `importFromOsmNode` method.
- Add example of new OSM import endpoint to `README` file.

## Changes by Grok Code

- Added OSM API functionality
- Remove outdated TODO comment in `PosServiceImpl.java`.
- Remove hardcoded test data in `OsmDataServiceImpl` to use real OpenStreetMap API data instead.

## Removed

- n/a
