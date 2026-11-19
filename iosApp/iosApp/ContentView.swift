import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ZStack {
            Color(red: 15.0 / 255.0, green: 15.0 / 255.0, blue: 18.0 / 255.0)
                .ignoresSafeArea()
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
