# CodeOnTheGo (CoGo) CLI Commands for Origin Studio

## Single Command (Recommended)
```bash
cogo build --project . --blueprint ./BLUEPRINT.md --profile origin-studio --reimplement --sync --assemble --sign --verify
```

## Two-Command Workflow (More Explicit)
```bash
cogo reimplement --project . --blueprint ./BLUEPRINT.md --force --preserve assets,keystore,local.properties
cogo build --project . --clean --assembleRelease --generate-metadata --verify-checksums
```

## Alternative with Direct Gradle (Fallback)
```bash
./gradlew clean assembleRelease bundleRelease && ./gradlew lint test
```

## Notes
- Use `assembleDebug` instead of `assembleRelease` if no keystore is configured
- The blueprint guides CoGo on preserving TermFold capabilities while implementing Origin Studio per spec
- All commands should be run from `.../CodeOnTheGoProjects/OriginStudio $`
