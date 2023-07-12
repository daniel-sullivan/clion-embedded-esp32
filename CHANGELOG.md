# OpenOCD + ESP32 Support for embedded development Changelog

## Unreleased

## 0.3.1

### Fixed
- Fix typo when restoring saved run configs

## 0.3.0

### Fixed
- Fix deadlock ([#4](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/4)) through PR ([#7](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/9))
- Allow not specifying a few config entries
- Support more reset types
- Cleanup (migrate away from obsolete and deprecated entities)

## 0.2.4

### Fixed
- Fix deadlock ([#4](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/4)) through PR ([#7](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/7))

## 0.2.3

### Fixed
- Fix file path parameter ([#6](https://github.com/ThexXTURBOXx/clion-embedded-esp32/issues/6))
- Fix icons
- Add ESP32 tag to run configurations to differentiate them better
- Change default command to `program_esp`
- Support new xpack OpenOCD format
- Fix changelog

## 0.2.2

### Fixed
- Updated to 2023.2 EAP

## 0.2.1

### Fixed
- Updated to 2023.1 EAP

## 0.2.0

### Added
- Support for both `program_esp[32]` commands ([#3](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/3))
- Support for `offset` parameters ([#3](https://github.com/ThexXTURBOXx/clion-embedded-esp32/pull/3))
- Support for `verify` parameter

## 0.1.9

### Fixed
- Updated to 2022.3 EAP

## 0.1.8

### Fixed
- Updated to 2022.2 EAP

## 0.1.7

### Fixed
- Updated to 2022.1 EAP again

## 0.1.6

### Fixed
- Fixed support for CLion 2020.3 and 2021.1

## 0.1.5

### Added
- Support for CLion 2022.1 EAP

## 0.1.4

### Changed
- Changed the icon

## 0.1.3

### Changed
- Migrate to Gradle

### Fixed
- Fixed `NoClassDefFoundError` (#6)

## 0.1.2

### Added
- Updated for CLion 2019.1+

## 0.1.1

### Added
- STM32G0 and STM32L5 experimental support added
