//
//  WorkflowManager.swift
//  App
//
//  Created by Alpha on 8/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import Foundation
import Alamofire
import AppAuth

import shared

class SwiftFormDataUploadDelegate : FormDataUploadDelegate {
  
    func uploadFormData(url: String, token: String, data: Any?, metadata: String, completionHandler: @escaping (String?, Error?) -> Void) {
        guard let data = data as? Data else {
            completionHandler(nil, NSError(domain: "", code: 1, userInfo: nil))
            return
        }
        
        let headers: HTTPHeaders = [
            .authorization(bearerToken: token),
            .contentType("multipart/form-data")
        ]
        
        AF.upload(multipartFormData: { multipartFormData in
            multipartFormData.append(data, withName: "data")
            multipartFormData.append(metadata.data(using: .utf8)!, withName: "metadata", fileName: nil, mimeType: "application/json")
        }, to: url, headers: headers)
            .responseString { response in
                completionHandler(response.value, response.error)
            }
  
    }
    
}

let formDataUploader = SwiftFormDataUploadDelegate()

let workflowManager = WorkflowManager<OIDTokenRequest, NSData>(
    preferences: NSObject(),
    databaseDriverFactory: DatabaseDriverFactory(),
    openIDHelperDelegate: openIDHelper,
    formDataUploadDelegate: formDataUploader
)
