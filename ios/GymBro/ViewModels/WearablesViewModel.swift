import MWDATCore
import SwiftUI

#if DEBUG
import MWDATMockDevice
#endif

@MainActor
class WearablesViewModel: ObservableObject {
    @Published var devices: [DeviceIdentifier]
    @Published var hasMockDevice: Bool
    @Published var registrationState: RegistrationState
    @Published var showGettingStartedSheet = false
    @Published var showError = false
    @Published var errorMessage = ""

    private var registrationTask: Task<Void, Never>?
    private var deviceStreamTask: Task<Void, Never>?
    private let wearables: WearablesInterface
    private var compatibilityListenerTokens: [DeviceIdentifier: AnyListenerToken] = [:]

    init(wearables: WearablesInterface) {
        self.wearables = wearables
        self.devices = wearables.devices
        self.hasMockDevice = false
        self.registrationState = wearables.registrationState

        deviceStreamTask = Task {
            for await devices in wearables.devicesStream() {
                self.devices = devices
                #if DEBUG
                self.hasMockDevice = !MockDeviceKit.shared.pairedDevices.isEmpty
                #endif
                self.monitorCompatibility(devices)
            }
        }

        registrationTask = Task {
            for await state in wearables.registrationStateStream() {
                let previous = self.registrationState
                self.registrationState = state
                if !self.showGettingStartedSheet && state == .registered && previous == .registering {
                    self.showGettingStartedSheet = true
                }
            }
        }
    }

    deinit {
        registrationTask?.cancel()
        deviceStreamTask?.cancel()
    }

    func connectGlasses() {
        guard registrationState != .registering else { return }
        Task {
            do {
                try await wearables.startRegistration()
            } catch let error as RegistrationError {
                showError(error.description)
            } catch {
                showError(error.localizedDescription)
            }
        }
    }

    func disconnectGlasses() {
        Task {
            do {
                try await wearables.startUnregistration()
            } catch let error as UnregistrationError {
                showError(error.description)
            } catch {
                showError(error.localizedDescription)
            }
        }
    }

    func handleURL(_ url: URL) {
        Wearables.shared.handleUrl(url)
    }

    func showError(_ message: String) {
        errorMessage = message
        showError = true
    }

    func dismissError() {
        showError = false
    }

    private func monitorCompatibility(_ devices: [DeviceIdentifier]) {
        let deviceSet = Set(devices)
        compatibilityListenerTokens = compatibilityListenerTokens.filter { deviceSet.contains($0.key) }

        for deviceId in devices {
            guard compatibilityListenerTokens[deviceId] == nil else { continue }
            guard let device = wearables.deviceForIdentifier(deviceId) else { continue }

            let deviceName = device.nameOrId()
            let token = device.addCompatibilityListener { [weak self] compatibility in
                if compatibility == .deviceUpdateRequired {
                    Task { @MainActor in
                        self?.showError("Device '\(deviceName)' requires an update.")
                    }
                }
            }
            compatibilityListenerTokens[deviceId] = token
        }
    }
}
