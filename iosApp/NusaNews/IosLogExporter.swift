import Foundation
import NusaNewsShared
import UIKit

/// iOS implementation of the shared `LogExporter` contract: Files / share sheet.
///
/// Nothing is uploaded and nothing is streamed. The rotating log directory written by `:shared` is
/// zipped on an explicit action and offered through `UIActivityViewController`, so the user decides
/// where it goes — AirDrop, Files, mail to support, or nowhere.
final class IosLogExporter: LogExporter {
    func exportBundle(onResult: @escaping (LogExportResult) -> Void) {
        DispatchQueue.global(qos: .utility).async {
            let (result, bundleURL) = self.buildBundle()
            DispatchQueue.main.async {
                if let bundleURL {
                    self.presentShareSheet(for: bundleURL)
                }
                onResult(result)
            }
        }
    }

    private func buildBundle() -> (LogExportResult, URL?) {
        guard let directoryPath = SharedModule_iosKt.sessionLogDirectory() else {
            return (LogExportResult.NothingToExport.shared, nil)
        }

        let directory = URL(fileURLWithPath: directoryPath, isDirectory: true)
        let contents = (try? FileManager.default.contentsOfDirectory(atPath: directoryPath)) ?? []
        guard !contents.isEmpty else { return (LogExportResult.NothingToExport.shared, nil) }

        // `.forUploading` hands back a zip of the directory; no third-party archiver is needed.
        var coordinatorError: NSError?
        var bundleURL: URL?
        var copyError: Error?

        NSFileCoordinator().coordinate(
            readingItemAt: directory,
            options: [.forUploading],
            error: &coordinatorError
        ) { zippedURL in
            let destination = FileManager.default.temporaryDirectory
                .appendingPathComponent("nusa-news-logs-\(Int(Date().timeIntervalSince1970)).zip")
            do {
                try? FileManager.default.removeItem(at: destination)
                try FileManager.default.copyItem(at: zippedURL, to: destination)
                bundleURL = destination
            } catch {
                copyError = error
            }
        }

        if let error = coordinatorError ?? copyError {
            return (LogExportResult.Failed(reason: error.localizedDescription), nil)
        }
        guard let bundleURL else {
            return (LogExportResult.Failed(reason: "Bundle was not produced"), nil)
        }

        let size = (try? FileManager.default
            .attributesOfItem(atPath: bundleURL.path)[.size] as? Int64) ?? 0
        guard size <= LogExportLimits.shared.MaxBundleBytes else {
            try? FileManager.default.removeItem(at: bundleURL)
            return (LogExportResult.Failed(reason: "Bundle exceeds the size cap"), nil)
        }

        let result = LogExportResult.Shared(fileName: bundleURL.lastPathComponent, byteCount: size)
        return (result, bundleURL)
    }

    private func presentShareSheet(for url: URL) {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        guard let root = scene?.keyWindow?.rootViewController else { return }

        let controller = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        controller.popoverPresentationController?.sourceView = root.view
        root.present(controller, animated: true)
    }
}
