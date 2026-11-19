import SwiftUI

@main
struct StationLiveApp: App {
    var body: some Scene {
        WindowGroup {
            ConnectionView()
                .preferredColorScheme(.dark)
        }
    }
}
