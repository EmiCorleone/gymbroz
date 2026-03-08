import SwiftUI
import SwiftData

struct DashboardView: View {
    let onStartWorkout: () -> Void
    @StateObject private var viewModel = DashboardViewModel()
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Hi, \(viewModel.state.userName.isEmpty ? "Athlete" : viewModel.state.userName)").font(.title.bold()).foregroundColor(AppColor.textPrimary)
                        Text(Date(), style: .date).font(.subheadline).foregroundColor(AppColor.textSecondary)
                    }
                    Spacer()
                    Circle().fill(AppColor.accent.opacity(0.2)).frame(width: 48, height: 48)
                        .overlay(Image(systemName: "person.fill").foregroundColor(AppColor.accent))
                        .glow(AppColor.accent, radius: 8)
                }.padding(.horizontal, 20).padding(.top, 20)

                HStack(spacing: 12) {
                    StatCard(title: "Workouts", value: "\(viewModel.state.totalWorkouts)", color: AppColor.accent)
                    StatCard(title: "Total Reps", value: "\(viewModel.state.totalReps)", color: AppColor.accentGreen)
                    StatCard(title: "Streak", value: "\(viewModel.state.currentStreak)d", color: AppColor.accentOrange)
                }.padding(.horizontal, 20)

                VStack(alignment: .leading, spacing: 8) {
                    Text("This Week").font(.headline).foregroundColor(AppColor.textPrimary)
                    HStack(spacing: 8) {
                        let days = ["M", "T", "W", "T", "F", "S", "S"]
                        ForEach(0..<7, id: \.self) { i in
                            VStack(spacing: 4) {
                                Circle().fill(viewModel.state.weeklyActivity[i] ? AppColor.accentGreen : AppColor.surfaceLight).frame(width: 32, height: 32)
                                Text(days[i]).font(.caption2).foregroundColor(AppColor.textSecondary)
                            }
                        }
                    }
                }.frame(maxWidth: .infinity, alignment: .leading).padding(16).glassCard().padding(.horizontal, 20)

                Button(action: onStartWorkout) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Start Workout").font(.title3.bold()).foregroundColor(AppColor.textPrimary)
                            Text("Connect your glasses and begin").font(.caption).foregroundColor(AppColor.textSecondary)
                        }
                        Spacer()
                        Image(systemName: "play.circle.fill").font(.system(size: 44)).foregroundColor(AppColor.accent)
                    }.padding(20).background(LinearGradient(colors: [AppColor.accent.opacity(0.15), AppColor.surface], startPoint: .leading, endPoint: .trailing))
                    .clipShape(RoundedRectangle(cornerRadius: 16)).overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColor.accent.opacity(0.3), lineWidth: 1))
                }.padding(.horizontal, 20)

                VStack(alignment: .leading, spacing: 12) {
                    Text("Recent Workouts").font(.headline).foregroundColor(AppColor.textPrimary)
                    if viewModel.state.recentSessions.isEmpty {
                        Text("No workouts yet. Start your first session!").foregroundColor(AppColor.textSecondary).padding(.vertical, 20)
                    } else {
                        ForEach(viewModel.state.recentSessions, id: \.startTime) { session in
                            HStack {
                                Circle().fill(AppColor.accent.opacity(0.2)).frame(width: 40, height: 40)
                                    .overlay(Image(systemName: "figure.strengthtraining.traditional").foregroundColor(AppColor.accent).font(.caption))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(session.totalReps) reps").font(.subheadline.bold()).foregroundColor(AppColor.textPrimary)
                                    Text("\(session.durationMinutes) min").font(.caption).foregroundColor(AppColor.textSecondary)
                                }
                                Spacer()
                                Text(session.startTime, style: .relative).font(.caption).foregroundColor(AppColor.textSecondary)
                            }.padding(12).glassCard()
                        }
                    }
                }.padding(.horizontal, 20)

                Spacer(minLength: 100)
            }
        }
        .background(AppColor.background)
        .onAppear { viewModel.setup(modelContext: modelContext) }
    }
}

private struct StatCard: View {
    let title: String; let value: String; let color: Color
    var body: some View {
        VStack(spacing: 8) {
            Text(value).font(.title2.bold()).foregroundColor(AppColor.textPrimary)
            Text(title).font(.caption).foregroundColor(AppColor.textSecondary)
        }.frame(maxWidth: .infinity).padding(.vertical, 16).glassCard().glow(color, radius: 6)
    }
}
