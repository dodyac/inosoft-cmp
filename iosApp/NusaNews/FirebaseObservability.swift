import Foundation
import NusaNewsShared

#if canImport(FirebaseCore)
import FirebaseCore
import FirebaseAnalytics
import FirebasePerformance
#endif

/// iOS side of the shared observability contracts.
///
/// Performance Monitoring and Analytics are not reachable from Kotlin/Native without cinterop, so
/// they are implemented here and injected into Koin at startup. Crash reporting is deliberately
/// *not* here: it lives in shared Kotlin via CrashKiOS, which is the only way an uncaught Kotlin
/// exception produces a usable stack trace. These are the only Swift types that touch Firebase.
///
/// Everything is guarded by `canImport` so the project still builds and runs before the Firebase
/// Swift packages are resolved — it just collects nothing until then.
enum FirebaseObservability {
    /// Call before `initKoinIos`. Firebase on Apple has no auto-initialising provider.
    static func configure() {
        #if canImport(FirebaseCore)
        guard FirebaseApp.app() == nil else { return }
        guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
            print("[NewsReaderSetup] GoogleService-Info.plist is missing; Firebase stays disabled.")
            return
        }
        FirebaseApp.configure()
        #endif
    }

    static var performanceTracer: PerformanceTracer {
        #if canImport(FirebaseCore)
        return FirebasePerformanceTracer()
        #else
        return NoOpPerformanceTracer.shared
        #endif
    }

    static var analyticsTracker: AnalyticsTracker {
        #if canImport(FirebaseCore)
        return FirebaseAnalyticsTracker()
        #else
        return NoOpAnalyticsTracker.shared
        #endif
    }
}

#if canImport(FirebaseCore)

private final class FirebaseTraceHandle: TraceHandle {
    private let trace: Trace
    private var stopped = false

    init(trace: Trace) {
        self.trace = trace
    }

    func putAttribute(key: String, value: String) {
        guard !stopped else { return }
        trace.setValue(String(value.prefix(100)), forAttribute: String(key.prefix(32)))
    }

    func putMetric(name: String, value: Int64) {
        guard !stopped else { return }
        trace.setValue(value, forMetric: String(name.prefix(32)))
    }

    func stop() {
        guard !stopped else { return }
        stopped = true
        trace.stop()
    }
}

private final class FirebasePerformanceTracer: PerformanceTracer {
    func startTrace(name: String) -> TraceHandle {
        guard let trace = Performance.startTrace(name: name) else {
            return NoOpTraceHandle.shared
        }
        return FirebaseTraceHandle(trace: trace)
    }
}

private final class FirebaseAnalyticsTracker: AnalyticsTracker {
    func track(event: String, params: [String: String]) {
        Analytics.logEvent(event, parameters: params.isEmpty ? nil : params)
    }
}

#endif
