import Foundation
import Supabase

final class GymBroSupabaseClient {
    static let shared = GymBroSupabaseClient()

    let client: SupabaseClient

    private init() {
        let url = SettingsManager.shared.supabaseURL.isEmpty
            ? Secrets.supabaseURL
            : SettingsManager.shared.supabaseURL
        let key = SettingsManager.shared.supabaseAnonKey.isEmpty
            ? Secrets.supabaseAnonKey
            : SettingsManager.shared.supabaseAnonKey

        client = SupabaseClient(
            supabaseURL: URL(string: url)!,
            supabaseKey: key
        )
    }
}
