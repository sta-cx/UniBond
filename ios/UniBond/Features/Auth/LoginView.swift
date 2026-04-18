import SwiftUI
import AuthenticationServices

struct LoginView: View {
    @State var viewModel: AuthViewModel

    var body: some View {
        ZStack {
            AppColors.backgroundGradient.ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                Circle()
                    .fill(AppColors.primaryGradient)
                    .frame(width: 80, height: 80)
                    .overlay(
                        Image(systemName: "heart.fill")
                            .font(.system(size: 36))
                            .foregroundStyle(.white)
                    )

                Text("UniBond")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppColors.textPrimary)
                Text("每日默默问好，让爱更近")
                    .font(.system(size: 15))
                    .foregroundStyle(AppColors.textSecondary)

                Spacer()

                switch viewModel.loginStep {
                case .initial:
                    initialButtons
                case .emailInput:
                    emailInputSection
                case .codeInput:
                    codeInputSection
                }

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.error)
                }

                Spacer().frame(height: 40)

                Text("登录即表示同意《用户协议》和《隐私政策》")
                    .font(.system(size: 11))
                    .foregroundStyle(AppColors.textSecondary)
            }
            .padding(.horizontal, 32)
        }
    }

    private var initialButtons: some View {
        VStack(spacing: 16) {
            SignInWithAppleButton(.signIn) { request in
                request.requestedScopes = [.fullName, .email]
            } onCompletion: { result in
                Task {
                    switch result {
                    case .success(let authorization):
                        guard
                            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                            let tokenData = credential.identityToken,
                            let token = String(data: tokenData, encoding: .utf8)
                        else { return }

                        await viewModel.loginWithApple(identityToken: token, nickname: credential.fullName?.givenName)
                    case .failure:
                        break
                    }
                }
            }
            .signInWithAppleButtonStyle(.black)
            .frame(height: 50)
            .clipShape(RoundedRectangle(cornerRadius: 24))

            divider

            SecondaryButton("邮箱验证码登录", icon: "envelope.fill") {
                viewModel.loginStep = .emailInput
            }
        }
    }

    private var emailInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入邮箱", text: Binding(get: { viewModel.email }, set: { viewModel.email = $0 }))
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("获取验证码", isDisabled: !viewModel.isEmailValid || viewModel.isLoading) {
                Task { await viewModel.sendCode() }
            }
        }
    }

    private var codeInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入 6 位验证码", text: Binding(get: { viewModel.code }, set: { viewModel.code = $0 }))
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("登录", isDisabled: !viewModel.isCodeValid || viewModel.isLoading) {
                Task { await viewModel.loginWithEmail() }
            }

            if viewModel.countdown > 0 {
                Text("重新发送 (\(viewModel.countdown)s)")
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.textSecondary)
            } else {
                Button("重新发送验证码") {
                    Task { await viewModel.sendCode() }
                }
                .font(.system(size: 13))
                .foregroundStyle(AppColors.primaryPurple)
            }
        }
    }

    private var divider: some View {
        HStack {
            Rectangle()
                .fill(AppColors.textSecondary.opacity(0.3))
                .frame(height: 0.5)
            Text("或")
                .font(.system(size: 13))
                .foregroundStyle(AppColors.textSecondary)
            Rectangle()
                .fill(AppColors.textSecondary.opacity(0.3))
                .frame(height: 0.5)
        }
    }
}
