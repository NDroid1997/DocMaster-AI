# Implementation Plan - AI Conversion Flow & Storage Redesign

Redesign the file conversion flow to include a result preview screen (especially for text) and ensure all outputs are saved to the public Downloads folder using `StoragePaths`.

## Proposed Changes

### Core Components

#### [ConversionRepository.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/data/repository/ConversionRepository.kt)
- Updated to use `saveToDownloads` for all conversion outputs.
- Integrates `StoragePaths.downloadsRelativePath()` for `MediaStore` saving.
- Maintains internal copies for UI preview and database consistency.
- Robust URI handling for source files.

#### [OpenAiService.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/data/remote/OpenAiService.kt)
- Singleton service for AI processing (currently simulated as requested).

### UI & Navigation

#### [NEW] [ConversionResultScreen.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/ui/screens/converter/ConversionResultScreen.kt)
- New screen to display conversion success.
- Shows extracted text preview for OCR/PDF-to-Text.
- Actions: Copy text, Share file, Back to Home.

#### [ConverterScreen.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/ui/screens/converter/ConverterScreen.kt)
- Added `onConversionComplete` callback to navigate to the result screen.
- Simplified UI by removing inline result cards in favor of the new screen.

#### [DocMasterNavHost.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/ui/navigation/DocMasterNavHost.kt)
- Added route for `Screen.ConversionResult`.

#### [Screen.kt](file:///Users/nityenhembram/Project/MyAndroidProjects/first project/DocMasterAI/app/src/main/java/com/ndroid/docmasterai/ui/navigation/Screen.kt)
- Added `ConversionResult(val resultPath: String)` to the `Screen` sealed interface.

## Verification Plan

### Manual Verification
- Run the app and navigate to **Convert**.
- Perform **PDF to Text** and **Image to Text** conversions.
- Verify navigation to `ConversionResultScreen`.
- Verify text preview and copy functionality.
- Check the device's `Downloads/DocMasterAi` folder for the output file.
- Perform image format conversions (e.g., PNG to JPG) and verify the file is saved correctly.
