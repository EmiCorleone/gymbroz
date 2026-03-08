#if DEBUG
import SwiftUI
import MWDATMockDevice

struct DebugMenuView: View {
    @ObservedObject var debugMenuViewModel: DebugMenuViewModel

    var body: some View {
        VStack {
            HStack {
                Spacer()
                Button(action: { debugMenuViewModel.showDebugMenu = true }) {
                    Image(systemName: "ladybug.fill")
                        .foregroundColor(.orange)
                        .padding(8)
                        .background(Color.black.opacity(0.5))
                        .clipShape(Circle())
                }
                .padding()
            }
            Spacer()
        }
    }
}

struct MockDeviceKitView: View {
    @ObservedObject var viewModel: MockDeviceKitViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List {
                Section("Mock Devices") {
                    ForEach(viewModel.pairedDevices, id: \.self) { device in
                        HStack {
                            Text("Mock Device")
                            Spacer()
                            Button("Connect") { viewModel.connectDevice(device) }
                                .buttonStyle(.bordered)
                        }
                    }
                }
                Section {
                    Button("Add Mock Device") { viewModel.pairDevice() }
                }
            }
            .navigationTitle("Debug Menu")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
#endif
