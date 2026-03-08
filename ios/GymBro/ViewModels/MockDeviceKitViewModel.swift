#if DEBUG
import SwiftUI
import MWDATMockDevice

@MainActor
class DebugMenuViewModel: ObservableObject {
    @Published var showDebugMenu = false
    let mockDeviceKitViewModel: MockDeviceKitViewModel

    init(mockDeviceKit: any MockDeviceKitInterface) {
        self.mockDeviceKitViewModel = MockDeviceKitViewModel(mockDeviceKit: mockDeviceKit)
    }
}

@MainActor
class MockDeviceKitViewModel: ObservableObject {
    @Published var pairedDevices: [any MockDevice] = []
    private let mockDeviceKit: any MockDeviceKitInterface

    init(mockDeviceKit: any MockDeviceKitInterface) {
        self.mockDeviceKit = mockDeviceKit
        self.pairedDevices = mockDeviceKit.pairedDevices
    }

    func pairDevice() {
        let device = mockDeviceKit.pairRaybanMeta()
        device.powerOn()
        device.don()
        pairedDevices = mockDeviceKit.pairedDevices
    }

    func unpairDevice(_ device: any MockDevice) {
        mockDeviceKit.unpairDevice(device)
        pairedDevices = mockDeviceKit.pairedDevices
    }

    func connectDevice(_ device: any MockDevice) {
        device.powerOn()
    }

    func disconnectDevice(_ device: any MockDevice) {
        device.powerOff()
    }
}
#endif
