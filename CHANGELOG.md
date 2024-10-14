# OpenOCD + ESP32 Support for embedded development Changelog

## [Unreleased]

## [0.4.0]

### Added

- Add additional program parameters ([#22](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/22)) through PR ([#23](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/23))

### Changed

- Update IntelliJ platform plugin

### Fixed

- Fix OpenOCD launching in wrong thread ([#21](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/21)) through PR ([#24](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/24))

## [0.3.8]

### Fixed

- Updated to 2024.2 EAP

## [0.3.7]

### Fixed

- Updated to 2024.1 EAP

## [0.3.6]

### Fixed

- Fixed program offsets not properly updating ([#15](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/15))

## [0.3.5]

### Fixed

- Fixed content roots

## [0.3.4]

### Added

- Add debugger configuration ([#12](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/12))

## [0.3.3]

### Fixed

- Updated to 2023.3 EAP
- Remove deprecations

## [0.3.2]

### Fixed

- Fix root path ([#10](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/10))

## [0.3.1]

### Fixed

- Fix typo when restoring saved run configs

## [0.3.0]

### Fixed

- Fix deadlock ([#4](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/4)) through PR ([#9](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/9))
- Allow not specifying a few config entries
- Support more reset types
- Cleanup (migrate away from obsolete and deprecated entities)

## [0.2.4]

### Fixed

- Fix deadlock ([#4](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/4)) through PR ([#7](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/7))

## [0.2.3]

### Fixed

- Fix file path parameter ([#6](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/6))
- Fix icons
- Add ESP32 tag to run configurations to differentiate them better
- Change default command to `program_esp`
- Support new xpack OpenOCD format
- Fix changelog

## [0.2.2]

### Fixed

- Updated to 2023.2 EAP

## [0.2.1]

### Fixed

- Updated to 2023.1 EAP

## [0.2.0]

### Added

- Support for both `program_esp[32]` commands ([#3](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/3))
- Support for `offset` parameters ([#3](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/3))
- Support for `verify` parameter

## [0.1.9]

### Fixed

- Updated to 2022.3 EAP

## [0.1.8]

### Fixed

- Updated to 2022.2 EAP

## [0.1.7]

### Fixed

- Updated to 2022.1 EAP again

## [0.1.6]

### Fixed

- Fixed support for CLion 2020.3 and 2021.1

## [0.1.5]

### Added

- Support for CLion 2022.1 EAP

## [0.1.4]

### Changed

- Changed the icon

## [0.1.3]

### Changed

- Migrate to Gradle

### Fixed

- Fixed `NoClassDefFoundError` (#6)

## [0.1.2]

### Added

- Updated for CLion 2019.1+

## [0.1.1]

### Added

- STM32G0 and STM32L5 experimental support added

[Unreleased]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.8...v0.4.0
[0.3.8]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.7...v0.3.8
[0.3.7]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.6...v0.3.7
[0.3.6]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.5...v0.3.6
[0.3.5]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.2.4...v0.3.0
[0.2.4]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.2.3...v0.2.4
[0.2.3]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.9...v0.2.0
[0.1.9]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.8...v0.1.9
[0.1.8]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/ThexXTURBOXx/clion-embedded-esp32/commits/v0.1.1
