import SwiftUI

struct ConnectionView: View {
    @State private var useM3U = false
    @State private var username = ""
    @State private var password = ""
    @State private var playlistURL = ""
    @State private var status = "Connection service is not configured yet."

    private let gold = Color(red: 0.84, green: 0.68, blue: 0.33)
    private let night = Color(red: 0.03, green: 0.04, blue: 0.07)

    var body: some View {
        ZStack {
            night.ignoresSafeArea()
            VStack(spacing: 22) {
                VStack(spacing: 5) {
                    Text("STATION LIVE").font(.system(size: 30, weight: .black)).foregroundStyle(gold)
                    Text("IPTV 4K").font(.title2.bold()).foregroundStyle(.white)
                    Text("ENTERTAINMENT, YOUR WAY").font(.caption2).tracking(2).foregroundStyle(.gray)
                }
                VStack(alignment: .leading, spacing: 16) {
                    Text("Connect to your service").font(.title2.bold()).foregroundStyle(.white)
                    Picker("Connection type", selection: $useM3U) {
                        Text("Xtream Codes").tag(false)
                        Text("M3U / M3U8").tag(true)
                    }.pickerStyle(.segmented)
                    if useM3U {
                        TextField("M3U / M3U8 playlist URL", text: $playlistURL)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.URL)
                    } else {
                        TextField("Username", text: $username)
                            .textInputAutocapitalization(.never)
                        SecureField("Password", text: $password)
                    }
                    Button {
                        status = useM3U
                            ? (playlistURL.isEmpty ? "Enter an M3U/M3U8 URL." : "Backend connection is not implemented yet.")
                            : (username.isEmpty || password.isEmpty ? "Enter your username and password." : "Backend connection is not implemented yet.")
                    } label: {
                        Text("CONTINUE").fontWeight(.heavy).frame(maxWidth: .infinity).padding()
                            .background(gold).foregroundStyle(night).clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    Text(status).font(.footnote).foregroundStyle(.gray)
                }
                .padding(22)
                .background(Color(red: 0.07, green: 0.10, blue: 0.15))
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .frame(maxWidth: 480)
                Spacer(minLength: 0)
            }.padding(24)
        }
    }
}
