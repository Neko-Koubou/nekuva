import SwiftUI
import Network

// Compatibility bridge for koin-compose-viewmodel 4.0.0 inline calls compiled against older lifecycle-viewmodel-compose ABI
@_silgen_name("kfun:androidx.lifecycle.viewmodel.compose#androidx_lifecycle_viewmodel_compose_LocalViewModelStoreOwner$stableprop_getter$artificial(){}kotlin.Int")
public func kmpLocalViewModelStoreOwnerStablePropGetter() -> Int32 {
    return 0
}

@_cdecl("ios_configure_doh")
public func ios_configure_doh(urlStringPtr: UnsafePointer<CChar>?) {
    guard let urlStringPtr = urlStringPtr else {
        nw_privacy_context_require_encrypted_name_resolution(_nw_privacy_context_default_context, false, nil)
        nw_privacy_context_flush_cache(_nw_privacy_context_default_context)
        return
    }
    let urlString = String(cString: urlStringPtr)
    let endpoint = nw_endpoint_create_url(urlString)
    let resolver = nw_resolver_config_create_https(endpoint)
    if urlString.contains("cloudflare") {
        let fb1 = nw_endpoint_create_host("1.1.1.1", "443")
        let fb2 = nw_endpoint_create_host("1.0.0.1", "443")
        nw_resolver_config_add_server_address(resolver, fb1)
        nw_resolver_config_add_server_address(resolver, fb2)
    } else if urlString.contains("google") {
        let fb1 = nw_endpoint_create_host("8.8.8.8", "443")
        let fb2 = nw_endpoint_create_host("8.8.4.4", "443")
        nw_resolver_config_add_server_address(resolver, fb1)
        nw_resolver_config_add_server_address(resolver, fb2)
    }
    nw_privacy_context_require_encrypted_name_resolution(_nw_privacy_context_default_context, true, resolver)
    nw_privacy_context_flush_cache(_nw_privacy_context_default_context)
}

@main
struct iOSApp: App {
    init() {
        // Apply Cloudflare DoH at app start by default so NSFW/blocked manga sources load without VPN
        "https://cloudflare-dns.com/dns-query".withCString {
            ios_configure_doh(urlStringPtr: $0)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
