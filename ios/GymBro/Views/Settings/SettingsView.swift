import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var settings: SettingsManager
    @State private var editingPrompt = ""
    @State private var showPromptEditor = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                SettingsSection(title: "Gemini API") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("API Key").font(.caption).foregroundColor(AppColor.textSecondary)
                        SecureField("Gemini API Key", text: $settings.geminiAPIKey).textFieldStyle(.roundedBorder)
                    }
                    Button("Edit System Prompt") { editingPrompt = settings.geminiSystemPrompt; showPromptEditor = true }.foregroundColor(AppColor.accent)
                }
                SettingsSection(title: "WebRTC") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Signaling URL").font(.caption).foregroundColor(AppColor.textSecondary)
                        TextField("wss://...", text: $settings.webrtcSignalingURL).textFieldStyle(.roundedBorder)
                    }
                }
                Button(action: { settings.resetAll() }) {
                    Text("Reset All Settings").foregroundColor(AppColor.error).frame(maxWidth: .infinity).padding(16).glassCard()
                }
                Spacer(minLength: 100)
            }.padding(.horizontal, 20).padding(.top, 20)
        }
        .background(AppColor.background).navigationTitle("Settings")
        .sheet(isPresented: $showPromptEditor) {
            NavigationView {
                TextEditor(text: $editingPrompt).padding().navigationTitle("System Prompt")
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showPromptEditor = false } }
                        ToolbarItem(placement: .confirmationAction) { Button("Save") { settings.geminiSystemPrompt = editingPrompt; showPromptEditor = false } }
                    }
            }
        }
    }
}

private struct SettingsSection<Content: View>: View {
    let title: String; @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) { Text(title).font(.headline).foregroundColor(AppColor.textPrimary); content }.padding(16).glassCard()
    }
}
