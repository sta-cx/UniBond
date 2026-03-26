import SwiftUI
import UIKit

struct BindPartnerView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: CoupleViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                CardView {
                    VStack(spacing: 12) {
                        Text("我的邀请码")
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                        Text(viewModel.myInviteCode)
                            .font(.system(size: 32, weight: .bold, design: .monospaced))
                            .foregroundStyle(AppColors.primaryPurple)
                            .kerning(4)

                        HStack(spacing: 16) {
                            Button {
                                UIPasteboard.general.string = viewModel.myInviteCode
                            } label: {
                                Label("复制", systemImage: "doc.on.doc")
                                    .font(.system(size: 13))
                            }

                            ShareLink(item: "来 UniBond 和我一起答题吧！我的邀请码：\(viewModel.myInviteCode)") {
                                Label("分享", systemImage: "square.and.arrow.up")
                                    .font(.system(size: 13))
                            }
                        }
                        .foregroundStyle(AppColors.primaryPurple)
                    }
                    .padding(20)
                }

                HStack {
                    Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
                    Text("或")
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.textSecondary)
                    Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
                }

                CardView {
                    VStack(spacing: 12) {
                        Text("输入对方邀请码")
                            .font(.system(size: 15, weight: .medium))
                        TextField("6 位邀请码", text: Binding(get: { viewModel.partnerCode }, set: { viewModel.partnerCode = String($0.prefix(6)).uppercased() }))
                            .font(.system(size: 20, weight: .semibold, design: .monospaced))
                            .multilineTextAlignment(.center)
                            .textInputAutocapitalization(.characters)
                            .padding(12)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(AppColors.primaryPurple.opacity(0.3), lineWidth: 1)
                            )

                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.error)
                        }

                        PrimaryButton("绑定 TA", icon: "link", isDisabled: viewModel.partnerCode.count != 6 || viewModel.isLoading) {
                            Task {
                                if await viewModel.bind() {
                                    router.homePath = NavigationPath()
                                }
                            }
                        }
                    }
                    .padding(20)
                }
            }
            .padding(20)
        }
        .gradientBackground()
        .navigationTitle("绑定伴侣")
    }
}
