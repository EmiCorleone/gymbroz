import SwiftUI
import SwiftData

struct OnboardingFlow: View {
    let onComplete: () -> Void
    @StateObject private var viewModel = OnboardingViewModel()
    @State private var currentStep = 0
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        ZStack {
            AppColor.background.ignoresSafeArea()
            AnimatedMeshGradientView().ignoresSafeArea().opacity(0.5)

            TabView(selection: $currentStep) {
                SplashStep(onNext: { currentStep = 1 }).tag(0)
                WelcomeStep(onGetStarted: { currentStep = 2 }, onSignIn: { viewModel.isSignUp = false; currentStep = 2 }).tag(1)
                EmailAuthStep(viewModel: viewModel, onNext: { currentStep = 3 }, onSkip: {
                    Task { await viewModel.signInAnonymously() }; currentStep = 3
                }).tag(2)
                NameStep(viewModel: viewModel, onNext: { currentStep = 4 }).tag(3)
                GenderStep(viewModel: viewModel, onNext: { currentStep = 5 }).tag(4)
                BodyStatsStep(viewModel: viewModel, onNext: { currentStep = 6 }).tag(5)
                FitnessGoalStep(viewModel: viewModel, onNext: { currentStep = 7 }).tag(6)
                ExperienceStep(viewModel: viewModel, onNext: { currentStep = 8 }).tag(7)
                WeeklyWorkoutsStep(viewModel: viewModel, onNext: { currentStep = 9 }).tag(8)
                CompletionStep(viewModel: viewModel, modelContext: modelContext, onComplete: onComplete).tag(9)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut, value: currentStep)
        }
    }
}

private struct SplashStep: View {
    let onNext: () -> Void
    @State private var appeared = false
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("GYMBRO").font(.system(size: 48, weight: .black)).foregroundColor(AppColor.accent).opacity(appeared ? 1 : 0)
            Text("Your AI Workout Partner").font(.title3).foregroundColor(AppColor.textSecondary).opacity(appeared ? 1 : 0)
            Spacer()
        }
        .onAppear {
            withAnimation(.easeIn(duration: 0.8)) { appeared = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { onNext() }
        }
    }
}

private struct WelcomeStep: View {
    let onGetStarted: () -> Void
    let onSignIn: () -> Void
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            Image(systemName: "figure.strengthtraining.traditional").font(.system(size: 80)).foregroundColor(AppColor.accent)
            Text("Welcome to GymBro").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            Text("AI-powered workout coaching through your Meta Ray-Ban glasses").font(.body).foregroundColor(AppColor.textSecondary).multilineTextAlignment(.center).padding(.horizontal, 32)
            Spacer()
            Button(action: onGetStarted) {
                Text("Get Started").font(.headline).frame(maxWidth: .infinity).padding().background(AppColor.accent).foregroundColor(.white).clipShape(RoundedRectangle(cornerRadius: 12))
            }.padding(.horizontal, 24)
            Button("Already have an account? Sign In", action: onSignIn).foregroundColor(AppColor.textSecondary).padding(.bottom, 32)
        }
    }
}

private struct EmailAuthStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    let onSkip: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text(viewModel.isSignUp ? "Create Account" : "Sign In").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            VStack(spacing: 16) {
                TextField("Email", text: $viewModel.email).textFieldStyle(.roundedBorder).keyboardType(.emailAddress).autocapitalization(.none)
                SecureField("Password", text: $viewModel.password).textFieldStyle(.roundedBorder)
            }.padding(.horizontal, 24)
            if let error = viewModel.authError { Text(error).foregroundColor(AppColor.error).font(.caption) }
            Button(action: { Task { await viewModel.authenticate(); if viewModel.authError == nil { onNext() } } }) {
                if viewModel.isAuthLoading { ProgressView().tint(.white) } else { Text(viewModel.isSignUp ? "Sign Up" : "Sign In") }
            }.font(.headline).frame(maxWidth: .infinity).padding().background(AppColor.accent).foregroundColor(.white).clipShape(RoundedRectangle(cornerRadius: 12)).padding(.horizontal, 24).disabled(viewModel.isAuthLoading)
            Button("Skip for now", action: onSkip).foregroundColor(AppColor.textSecondary)
            Spacer()
        }
    }
}

private struct NameStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("What's your name?").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            TextField("Your name", text: $viewModel.name).textFieldStyle(.roundedBorder).padding(.horizontal, 24)
            Spacer()
            OnboardingNextButton(enabled: !viewModel.name.isEmpty, action: onNext)
        }
    }
}

private struct GenderStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("Gender").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            ForEach(["Male", "Female", "Other"], id: \.self) { option in
                SelectionButton(title: option, isSelected: viewModel.gender == option) { viewModel.gender = option }
            }
            Spacer()
            OnboardingNextButton(enabled: !viewModel.gender.isEmpty, action: onNext)
        }.padding(.horizontal, 24)
    }
}

private struct BodyStatsStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("Body Stats").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            LabeledTextField(label: "Age", text: $viewModel.age, keyboardType: .numberPad)
            LabeledTextField(label: "Height (cm)", text: $viewModel.heightCm, keyboardType: .numberPad)
            LabeledTextField(label: "Weight (kg)", text: $viewModel.weightKg, keyboardType: .numberPad)
            Spacer()
            OnboardingNextButton(enabled: !viewModel.age.isEmpty && !viewModel.heightCm.isEmpty && !viewModel.weightKg.isEmpty, action: onNext)
        }.padding(.horizontal, 24)
    }
}

private struct FitnessGoalStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    let goals = [("build_muscle", "Build Muscle", "figure.strengthtraining.traditional"), ("lose_weight", "Lose Weight", "flame.fill"), ("stay_active", "Stay Active", "figure.walk"), ("improve_health", "Improve Health", "heart.fill")]
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("What's your goal?").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            ForEach(goals, id: \.0) { g in
                SelectionButton(title: g.1, icon: g.2, isSelected: viewModel.fitnessGoal == g.0) { viewModel.fitnessGoal = g.0 }
            }
            Spacer()
            OnboardingNextButton(enabled: !viewModel.fitnessGoal.isEmpty, action: onNext)
        }.padding(.horizontal, 24)
    }
}

private struct ExperienceStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("Experience Level").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            ForEach(["beginner", "intermediate", "advanced"], id: \.self) { level in
                SelectionButton(title: level.capitalized, isSelected: viewModel.experienceLevel == level) { viewModel.experienceLevel = level }
            }
            Spacer()
            OnboardingNextButton(enabled: !viewModel.experienceLevel.isEmpty, action: onNext)
        }.padding(.horizontal, 24)
    }
}

private struct WeeklyWorkoutsStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let onNext: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("Weekly Workouts").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            Text("How many times per week?").foregroundColor(AppColor.textSecondary)
            ForEach(["0-2", "3-5", "6+"], id: \.self) { opt in
                SelectionButton(title: "\(opt) days", isSelected: viewModel.weeklyWorkouts == opt) { viewModel.weeklyWorkouts = opt }
            }
            Spacer()
            OnboardingNextButton(enabled: !viewModel.weeklyWorkouts.isEmpty, action: onNext)
        }.padding(.horizontal, 24)
    }
}

private struct CompletionStep: View {
    @ObservedObject var viewModel: OnboardingViewModel
    let modelContext: ModelContext
    let onComplete: () -> Void
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            Image(systemName: "checkmark.circle.fill").font(.system(size: 80)).foregroundColor(AppColor.accentGreen)
            Text("You're All Set!").font(.title.bold()).foregroundColor(AppColor.textPrimary)
            Text("Let's start your fitness journey").foregroundColor(AppColor.textSecondary).multilineTextAlignment(.center)
            Spacer()
            Button(action: { Task { let ok = await viewModel.saveProfile(modelContext: modelContext); if ok { onComplete() } } }) {
                if viewModel.isSaving { ProgressView().tint(.white) } else { Text("Let's Go!") }
            }.font(.headline).frame(maxWidth: .infinity).padding().background(AppColor.accentGreen).foregroundColor(.white).clipShape(RoundedRectangle(cornerRadius: 12)).padding(.horizontal, 24).disabled(viewModel.isSaving).padding(.bottom, 32)
        }
    }
}

// MARK: - Reusable

private struct OnboardingNextButton: View {
    let enabled: Bool; let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text("Next").font(.headline).frame(maxWidth: .infinity).padding().background(enabled ? AppColor.accent : AppColor.surfaceLight).foregroundColor(.white).clipShape(RoundedRectangle(cornerRadius: 12))
        }.disabled(!enabled).padding(.horizontal, 24).padding(.bottom, 32)
    }
}

private struct SelectionButton: View {
    let title: String; var icon: String? = nil; let isSelected: Bool; let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack {
                if let icon { Image(systemName: icon).foregroundColor(isSelected ? AppColor.accent : AppColor.textSecondary) }
                Text(title).foregroundColor(isSelected ? AppColor.textPrimary : AppColor.textSecondary)
                Spacer()
                if isSelected { Image(systemName: "checkmark.circle.fill").foregroundColor(AppColor.accent) }
            }.padding().background(isSelected ? AppColor.accent.opacity(0.15) : AppColor.surface).clipShape(RoundedRectangle(cornerRadius: 12)).overlay(RoundedRectangle(cornerRadius: 12).stroke(isSelected ? AppColor.accent.opacity(0.5) : AppColor.cardBorder, lineWidth: 1))
        }
    }
}

private struct LabeledTextField: View {
    let label: String; @Binding var text: String; var keyboardType: UIKeyboardType = .default
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundColor(AppColor.textSecondary)
            TextField(label, text: $text).keyboardType(keyboardType).textFieldStyle(.roundedBorder)
        }
    }
}
