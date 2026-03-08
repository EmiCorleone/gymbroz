#if DEBUG
import SwiftUI
import MWDATMockDevice

@MainActor
class DebugMenuViewModel: ObservableObject {
    @Published var showDebugMenu = false
    let mockDeviceKitViewModel: MockDeviceKitViewModel

    init(mockDeviceKit: MockDeviceKit) {
        self.mockDeviceKitViewModel = MockDeviceKitViewModel(mockDeviceKit: mockDeviceKit)
    }
}

@MainActor
class MockDeviceKitViewModel: ObservableObject {
    @Published var pairedDevices: [MockDevice] = []
    private let mockDeviceKit: MockDeviceKit

    init(mockDeviceKit: MockDeviceKit) {
        self.mockDeviceKit = mockDeviceKit
        self.pairedDevices = mockDeviceKit.pairedDevices
    }

    func pairDevice() {
        let device = mockDeviceKit.createDevice()
        mockDeviceKit.pair(device)
        pairedDevices = mockDeviceKit.pairedDevices
    }

    func unpairDevice(_ device: MockDevice) {
        mockDeviceKit.unpair(device)
        pairedDevices = mockDeviceKit.pairedDevices
    }

    func connectDevice(_ device: MockDevice) {
        device.connect()
    }

    func disconnectDevice(_ device: MockDevice) {
        device.disconnect()
    }
}
#endif
