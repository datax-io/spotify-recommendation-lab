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
import SwiftCSV

import shared

extension String: LocalizedError {
    public var errorDescription: String? { return self }
}

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

        guard let encodedData = data.base64EncodedString().data(using: .utf8) else {
            print("Unable to convert data to encoded data")
            completionHandler(nil, NSError(domain: "", code: 1, userInfo: nil))
            return
        }
        
        AF.upload(multipartFormData: { multipartFormData in
            multipartFormData.append(encodedData, withName: "data")
            multipartFormData.append(metadata.data(using: .utf8)!, withName: "metadata", fileName: nil, mimeType: "application/json")
        }, to: url, headers: headers)
            .responseString { response in
                completionHandler(response.value, response.error)
            }
  
    }
    
}

let formDataUploader = SwiftFormDataUploadDelegate()

class SwiftRemoteDataFetcher : RemoteDataFetcher {
    
    func fetchCsv(url: String, completionHandler: @escaping ([[String : String]]?, Error?) -> Void) {
        let request = AF.request(url)
        request.responseString { response in
            do {
                guard let csvString = response.value else {
                    completionHandler(nil, "Error")
                    return
                }
                let csv = try CSV(string: csvString)
                completionHandler(csv.namedRows, nil)
            } catch let error as NSError {
                completionHandler(nil, "Error")
            }
        }
    }
}

let remoteDataFetcher = SwiftRemoteDataFetcher()

let workflowManager = WorkflowManager<OIDTokenRequest, NSData>(
    preferences: NSObject(),
    databaseDriverFactory: DatabaseDriverFactory(),
    openIDHelperDelegate: openIDHelper,
    formDataUploadDelegate: formDataUploader,
    remoteDataFetcher: remoteDataFetcher
)
