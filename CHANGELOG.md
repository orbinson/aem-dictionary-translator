# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.6.0] - 2025-07-26

### Fixed

- Rate limiting leading to a 503 in AEMaaCS during publication [#228](https://github.com/orbinson/aem-dictionary-translator/issues/228)

## [1.5.1] - 2025-07-23

### Fixed

- `/apps/cq/core/content/nav/tools` is created with wrong jcr:primaryType [#232](https://github.com/orbinson/aem-dictionary-translator/issues/232)
- Creating/Updating keys doesn't work in AEM 6.5 due to "checkbox.show is not a function" [#231](https://github.com/orbinson/aem-dictionary-translator/issues/231)

## [1.5.0] - 2025-06-19

### Fixed

- Deleting a key that doesn't exist in all sibling dictionaries (i.e. not have a sling:MessageEntry node for one of the languages) produces an error [#207](https://github.com/orbinson/aem-dictionary-translator/issues/207)
- Editing a key containing newlines (\\n) produces duplicate entries [#208](https://github.com/orbinson/aem-dictionary-translator/issues/208)
- Importing a key that has the same name as a sling:MessageEntry node generates an error [#209](https://github.com/orbinson/aem-dictionary-translator/issues/209)

### Changed

- Allow empty value on i18n key [#172](https://github.com/orbinson/aem-dictionary-translator/issues/172)
- Improve exception handling in servlets [#212](https://github.com/orbinson/aem-dictionary-translator/issues/212)

## [1.4.1] - 2025-06-04

### Fixed

- Some labels in the edit messages dialog show "und" [#204](https://github.com/orbinson/aem-dictionary-translator/issues/204)

### Changed

- Improve speed of CombiningMessageEntryResourceProvider [#202](https://github.com/orbinson/aem-dictionary-translator/issues/202)

## [1.4.0] - 2025-05-31

### Fixed

- Dictionaries in /apps and /libs should not be marked as editable in AEMaaCS [#132](https://github.com/orbinson/aem-dictionary-translator/issues/132)
- Export logic was not executable twice [#170](https://github.com/orbinson/aem-dictionary-translator/issues/170)
- CombiningMessageEntryDatasource returns too many items for dialog [#174](https://github.com/orbinson/aem-dictionary-translator/issues/174)
- Add missing unit tests for data source [#136](https://github.com/orbinson/aem-dictionary-translator/issues/136)
- Support multiple base names per language, not one per dictionary [#163](https://github.com/orbinson/aem-dictionary-translator/issues/163)
- DictionaryServiceImpl does not handle underscored locale formats [#183](https://github.com/orbinson/aem-dictionary-translator/issues/183)
- Pagination inside dictionaries breaks sorting [#179](https://github.com/orbinson/aem-dictionary-translator/issues/179)
- Leading/trailing and multiple subsequent spaces are not properly escaped and therefore swallowed [#187](https://github.com/orbinson/aem-dictionary-translator/issues/187)

### Added

- Support JSON file based dictionaries in read-only mode [#26](https://github.com/orbinson/aem-dictionary-translator/issues/26)
- Expose conflicting dictionary entries [#161](https://github.com/orbinson/aem-dictionary-translator/issues/161)

### Changed

- Speed up the lookup of language resources [#182](https://github.com/orbinson/aem-dictionary-translator/issues/182)

## [1.3.3] - 2025-03-07

### Fixed

- Lazy loading of data in UI to prevent performance issues with large dictionaries [#80](https://github.com/orbinson/aem-dictionary-translator/issues/80)
- Publication of dictionaries recusively publishes all dictionary properties and items [#119](https://github.com/orbinson/aem-dictionary-translator/issues/102)
- Betty bar navigation does not crash tab [#113](https://github.com/orbinson/aem-dictionary-translator/issues/133)

### Changed

- Publication will always be batched per 100 [#119](https://github.com/orbinson/aem-dictionary-translator/issues/102) 

## [1.3.2] - 2025-02-22

### Fixed

- Delimiter recognition when both `,` and `;` are present in a file [#118](https://github.com/orbinson/aem-dictionary-translator/issues/118)
- Import for files with CR LF line endings [#119](https://github.com/orbinson/aem-dictionary-translator/issues/119)

## [1.3.1] - 2025-02-06

### Fixed

- Fixed "sling:Message" mixinType when it didn't exist at all as sling:MessageEntry [#106](https://github.com/orbinson/aem-dictionary-translator/issues/106)

## [1.3.0] - 2025-02-04

### Added

- AEM 6.5 support [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)
- Prerequisites for AEM and Java added in README.md [#65](https://github.com/orbinson/aem-dictionary-translator/issues/65)
- Make dictionary keys sortable [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)
- Support sling:key being optional [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)
- Render entries when not all languages are present [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)
- Enable "sling:Message" mixinType based message entries [#106](https://github.com/orbinson/aem-dictionary-translator/issues/106)

### Fixed

- Compilation of dictionary clientlib [#61](https://github.com/orbinson/aem-dictionary-translator/issues/61)
- Action bar for read only dictionaries shows only valid actions [#42](https://github.com/orbinson/aem-dictionary-translator/issues/42)
- Publish buttons are only visible for enabled agents [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)

### Changed

- Only single dictionary can be selected [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)
- Last modified date is removed from UI as it was not correct

## [1.2.1] - 2024-11-26

### Fixed

- Creation of new dictionary and editing of labels [#62](https://github.com/orbinson/aem-dictionary-translator/issues/62)

## [1.2.0] - 2024-11-26

### Changed

- Add button to default AEM translator (if it exists) [#47](https://github.com/orbinson/aem-dictionary-translator/issues/47)
- Consolidate field labels in dialog for "Create Key" and "Edit Key" [#57](https://github.com/orbinson/aem-dictionary-translator/issues/57)
- Language drop-down should expose both language and optionally country name [#56](https://github.com/orbinson/aem-dictionary-translator/issues/56)
- Update terminology to message entry and key instead of label [#33](https://github.com/orbinson/aem-dictionary-translator/issues/33)
- Add option to publish to preview [#43](https://github.com/orbinson/aem-dictionary-translator/issues/43)
- Add `/conf` as editable root for dictionaries [#46](https://github.com/orbinson/aem-dictionary-translator/issues/46)
- Update maven dependencies [#51](https://github.com/orbinson/aem-dictionary-translator/pull/51)
- Sort dictionary languages alphanumerically [#52](https://github.com/orbinson/aem-dictionary-translator/pull/52)

### Fixed

- Remove shortcut `d` for "Delete Language" as it conflicts with `Cmd + Opt + I` on Mac OS Chrome (Open Developer Tools)
- Fix publication error when too many keys are selected [#45](https://github.com/orbinson/aem-dictionary-translator/issues/45)
- Fix publication timeout when for large dictionaries [#44](https://github.com/orbinson/aem-dictionary-translator/issues/44)

## [1.1.4] - 2024-09-16

### Fixed

- Language nodes can be different than the jcr:language property value [#37](https://github.com/orbinson/aem-dictionary-translator/issues/37)
- Allow nt:folder as language node [#38](https://github.com/orbinson/aem-dictionary-translator/issues/38)
- Language nodes can be names different between the languages [#39](https://github.com/orbinson/aem-dictionary-translator/issues/39)
- Update all responses to use HTML based error responses so that Granite UI shows them in the dialogs [#24](https://github.com/orbinson/aem-dictionary-translator/issues/24)
- Don't swallow the replication exceptions, but show the user that it does not have the rights [#23](https://github.com/orbinson/aem-dictionary-translator/issues/23)

## [1.1.3] - 2024-08-03

### Fixed

- Add sling:key to message entries for a newly created language [#28](https://github.com/orbinson/aem-dictionary-translator/issues/28)

## [1.1.2] - 2024-02-28

### Fixed

- Unpublish items before deleting them [#21](https://github.com/orbinson/aem-dictionary-translator/issues/21)

## [1.1.1] - 2024-02-08

### Added

- CSV files with other languages than the dictionary currently has will not be imported [#17](https://github.com/orbinson/aem-dictionary-translator/pull/17)

### Fixed

- Resolved errors occurring when publishing message entries [#13](https://github.com/orbinson/aem-dictionary-translator/issues/13)
- Reactivated publish button for dictionaries [#13](https://github.com/orbinson/aem-dictionary-translator/issues/13)
- Exporting a csv sometimes gives completely empty file [#15](https://github.com/orbinson/aem-dictionary-translator/issues/15)
- Importing a csv with the correct languages sometimes still gives the error of wrong languages [#19](https://github.com/orbinson/aem-dictionary-translator/issues/19)

## [1.1.0] - 2024-02-01

### Changed

- Dictionaries with .json files are no longer showed in dictionary
  list: [#8](https://github.com/orbinson/aem-dictionary-translator/pull/5)
- Message entries with no translation for a specific language no longer have a
  sling:message so the value won't be empty but will have a fallback
  from another language or the key itself [#12](https://github.com/orbinson/aem-dictionary-translator/pull/12)

### Added

- Publish specific message entries in a dictionary [#8](https://github.com/orbinson/aem-dictionary-translator/pull/5)
- Export/Import CSV files of/to dictionaries [#10](https://github.com/orbinson/aem-dictionary-translator/issues/10)

## [1.0.6] - 2023-09-12

### Changed

- Add breadcrumbs to the dictionary page [#3](https://github.com/orbinson/aem-dictionary-translator/issues/3)

## [1.0.5] - 2023-08-30

### Fixed

- Fix message entries not working anymore when a new language is added

## [1.0.4] - 2023-08-16

### Fixed

- Use the distribution API to be able to deep replicate a
  dictionary [#1](https://github.com/orbinson/aem-dictionary-translator/pull/1)

## [1.0.3] - 2023-08-14

### Changed

- Set minimum required SDK version to 2023.6.12255.20230608T053118Z-230400

## [1.0.2] - 2023-08-11

### Fixed

- When adding new languages existing message entries could not be updated

### Changed

- Update README.md documentation

## [1.0.1] - 2023-08-10

### Fixed

- Use correct user mapping for service user

## [1.0.0] - 2023-08-09

### Added

- Create, publish and edit dictionaries
- Create, delete and update message entries in a dictionary

[unreleased]: https://github.com/orbinson/aem-dictionary-translator/compare/1.6.0...HEAD
[1.6.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.5.1...1.6.0
[1.5.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.5.2...1.5.1
[1.5.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.5.1...1.5.2
[1.5.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.5.0...1.5.1
[1.5.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.4.1...1.5.0
[1.4.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.4.0...1.4.1
[1.4.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.3.3...1.4.0
[1.3.3]: https://github.com/orbinson/aem-dictionary-translator/compare/1.3.2...1.3.3
[1.3.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.3.1...1.3.2
[1.3.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.3.0...1.3.1
[1.3.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.2.1...1.3.0
[1.2.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.4...1.2.0
[1.1.4]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.3...1.1.4
[1.1.3]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.6...1.1.0
[1.0.6]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.5...1.0.6
[1.0.5]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/orbinson/aem-dictionary-translator/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/orbinson/aem-dictionary-translator/compare/aef9658ce0967039de44f69228c16744d45e2764...1.0.0
