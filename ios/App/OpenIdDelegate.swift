//:/  OpenIdDelegate.swift
//  App
//
//  Created by Alpha on 24/12/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import Foundation
import shared
import AppAuth

var nonce: String? = nil
var codeVerifier: String? = nil


let serviceConfig = OIDServiceConfiguration(
    authorizationEndpoint: URL(string: "https://auth.oasislabs.com/oauth/authorize")!,
    tokenEndpoint: URL(string: "https://auth.oasislabs.com/oauth/token")!
)

class OpenIDHelper : OpenIDHelperDelegate {
    
    let config : OIDServiceConfiguration
    
    init(config: OIDServiceConfiguration){
        self.config=config
    }

    func getUri(clientId: String, redirectUri: String, scopes: [String]) -> String{
        let request = OIDAuthorizationRequest(
            configuration: serviceConfig,
            clientId: clientId,
            scopes: scopes,
            redirectURL: URL(string: redirectUri)!,
            responseType: "code",
            additionalParameters: [
                "audience": "https://api.oasislabs.com/parcel"
            ]
        )
    
        nonce = request.nonce
        codeVerifier = request.codeVerifier
        
    return request.authorizationRequestURL().absoluteString
    }

    func getTokenRequest(clientId: String, authCode: String) -> Any? {
        return OIDTokenRequest(
        configuration: serviceConfig,
        grantType: OIDGrantTypeAuthorizationCode,
        authorizationCode: authCode,
        redirectURL: URL(string: "https://storage.googleapis.com/datax-research-public/parcel-redirect/index.html")!,
        clientID: clientId,
        clientSecret: nil,
        scope: nil,
        refreshToken: nil,
        codeVerifier: codeVerifier,
        additionalParameters: [
            "audience": "https://api.oasislabs.com/parcel"
        ]
    )
    }

}

let openIDHelper = OpenIDHelper(config: serviceConfig)
