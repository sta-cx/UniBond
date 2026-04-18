import SwiftUI

struct ProfileView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router
    @State var viewModel: ProfileViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(spacing: 12) {
                    Circle()
                        .fill(AppColors.primaryGradient)
                        .frame(width: 72, height: 72)
                        .overlay(
                            Text(String((appState.currentUser?.nickname ?? "U").prefix(1)))
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(.white)
                        )

                    if viewModel.isEditing {
                        HStack {
                            TextField("昵称", text: Binding(get: { viewModel.nickname }, set: { viewModel.nickname = $0 }))
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 150)
                            Button("保存") {
                                Task { await viewModel.updateNickname() }
                            }
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(AppColors.primaryPurple)
                        }
                    } else {
                        HStack(spacing: 6) {
                            Text(appState.currentUser?.nickname ?? "")
                                .font(.system(size: 20, weight: .semibold))
                            Button {
                                viewModel.isEditing = true
                            } label: {
                                Image(systemName: "pencil")
                                    .font(.system(size: 13))
                                    .foregroundStyle(AppColors.textSecondary)
                            }
                        }
                    }

                    if let email = appState.currentUser?.email {
                        Text(email)
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                    }
                }
                .padding(.top, 12)

                if appState.isBound, let couple = appState.currentCouple {
                    CardView {
                        VStack(spacing: 12) {
                            HStack {
                                Text("💑").font(.system(size: 24))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("与 \(couple.partnerNickname ?? "TA") 已绑定")
                                        .font(.system(size: 15, weight: .medium))
                                    if let days = viewModel.daysTogether {
                                        Text("在一起 \(days) 天")
                                            .font(.system(size: 13))
                                            .foregroundStyle(AppColors.textSecondary)
                                    }
                                }
                                Spacer()
                            }
                            HStack {
                                Text("邀请码：\(appState.currentUser?.inviteCode ?? "")")
                                    .font(.system(size: 13, design: .monospaced))
                                    .foregroundStyle(AppColors.textSecondary)
                                Spacer()
                            }
                        }
                        .padding(16)
                    }
                } else {
                    CardView {
                        VStack(spacing: 12) {
                            Text("💕").font(.system(size: 32))
                            Text("绑定伴侣，开启默契之旅")
                                .font(.system(size: 15))
                                .foregroundStyle(AppColors.textSecondary)
                            PrimaryButton("去绑定", icon: "link") {
                                router.profilePath.append(AppRoute.bindPartner)
                            }
                        }
                        .padding(16)
                    }
                }

                CardView {
                    VStack(spacing: 0) {
                        settingsRow(icon: "bell.fill", title: "通知设置") {}
                        Divider().padding(.leading, 44)

                        if appState.isBound {
                            settingsRow(icon: "lock.shield.fill", title: "隐私") {}
                            Divider().padding(.leading, 44)
                            settingsRow(icon: "bubble.left.fill", title: "意见反馈") {}
                            Divider().padding(.leading, 44)
                        }

                        settingsRow(icon: "info.circle.fill", title: "关于 UniBond") {}
                        Divider().padding(.leading, 44)

                        if appState.isBound {
                            settingsRow(icon: "heart.slash.fill", title: "解绑伴侣", color: .orange) {
                                router.activeSheet = .unbindConfirm
                            }
                            Divider().padding(.leading, 44)
                        }

                        settingsRow(icon: "trash.fill", title: "删除账号", color: AppColors.error) {
                            viewModel.showDeleteConfirm = true
                        }
                    }
                }

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.error)
                }

                SecondaryButton("退出登录") {
                    Task { await viewModel.logout() }
                }
                .padding(.top, 8)
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .alert("确认删除账号？", isPresented: Binding(get: { viewModel.showDeleteConfirm }, set: { viewModel.showDeleteConfirm = $0 })) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) {
                Task { await viewModel.deleteAccount() }
            }
        } message: {
            Text("删除后所有数据将被永久清除，无法恢复。")
        }
    }

    private func settingsRow(icon: String, title: String, color: Color = AppColors.textPrimary, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 16))
                    .foregroundStyle(color)
                    .frame(width: 24)
                Text(title)
                    .font(.system(size: 15))
                    .foregroundStyle(color)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundStyle(AppColors.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
    }
}
