//
//  ViewController.swift
//  spotify-recommendation-lab
//
//  Created by Alpha on 3/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import UIKit
import shared
import SwiftSyft
import AppAuth
import Alamofire

class AuthViewController: UIViewController, WorkflowManagerCallback {
    
    @IBOutlet weak var getSpotifyAuthorizationButton: UIButton!
    @IBOutlet weak var currentSpotifyUserLabel: UILabel!
    
    @IBOutlet weak var fetchHistoryButton: UIButton!
    @IBOutlet weak var historyResultLabel: UILabel!
    @IBOutlet weak var fetchHistoryActivityIndicator: UIActivityIndicatorView!

    @IBOutlet weak var getParcelAuthorizationButton: UIButton!
    @IBOutlet weak var currentParcelUserLabel: UILabel!

    @IBOutlet weak var startTrainingButton: UIButton!
    @IBOutlet weak var startTrainingStatusLabel: UILabel!
    
    @IBOutlet weak var uploadDummyDocumentButton: UIButton!
    
    var token: String?
    static var tokenKey = "token"
        
    override func viewDidLoad() {
        super.viewDidLoad()
        workflowManager.callback = self

        self.startTrainingStatusLabel.text = "PyGrid: \(workflowManager.pygridHelper.host)"

    }
    
    @IBAction
    func getSpotifyAuthorization() {
        UIApplication.shared.open(URL(string: workflowManager.spotifyHelper.getAuthUrl())!)
    }
    
    func handleSpotifyAuthorization(_ fragment: String) {
        workflowManager.spotifyHelper.handleAuthResult(fragment: fragment) { _, _ in }
    }
    
    @IBAction func getParcelAuthorization(_ sender: Any) {
        UIApplication.shared.open(URL(string: workflowManager.parcelHelper.getAuthUrl())!)
    }

    func handleParcelAuthorization(_ fragment: String) {
        guard let request = OpenIDHelper.shared.getTokenRequest(clientId: workflowManager.parcelHelper.clientId, authCode: fragment) as? OIDTokenRequest else {
            return
        }
        OIDAuthorizationService.perform(request) { response, error in
            guard let token = response?.accessToken else {
                print(response)
                print(error)
                return
            }
            workflowManager.parcelHelper.handleAuthResult(fragment: token) { _, _ in }
        }

    }

    func onSpotifyUserChanged(user: SpotifyUser?) {
        guard let user = user else {
            self.currentSpotifyUserLabel.text = "Unauthorized"
            self.fetchHistoryButton.isEnabled = false
            return
        }
        self.currentSpotifyUserLabel.text = "User: \(user.displayName ?? user.id)"
        self.fetchHistoryButton.isEnabled = true
    }

    func onParcelUserChanged(user: ParcelUser?) {
        guard let user = user else {
            self.currentParcelUserLabel.text = "Unauthorized"
            return
        }
        self.currentParcelUserLabel.text = "User: \(user.id)"
    }

    @IBAction func fetchHistory(_ sender: Any) {
        self.fetchHistoryButton.isEnabled = false
        self.historyResultLabel.text = "Fetching preference data..."
        self.fetchHistoryActivityIndicator.startAnimating()
        workflowManager.fetchHistory() { result, _ in
            print(result)
        }
    }
    
    func onSpotifyHistoryStatusChanged(status: SpotifyHistoryStatus?) {
        self.fetchHistoryActivityIndicator.stopAnimating()
        guard let count = status?.trackCount else {
            self.historyResultLabel.text = "No result"
            return
        }
        self.historyResultLabel.text = "Result: \(count) tracks"
    }
    
    func onTrainingReadinessChanged(ready: Bool) {
        self.startTrainingButton.isEnabled = ready
        self.uploadDummyDocumentButton.isEnabled = ready
    }

    @IBAction func uploadDocument(_ sender: Any) {
        guard let data = "Test content".data(using: .utf8) else {
            return
        }
        workflowManager.parcelHelper.uploadDocument(data: NSData(data: data), fileName: "test.txt") { document, error in
            print(document)
        }
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        let vc = segue.destination
        switch vc {
        case let trainingVC as TrainingViewController:
            trainingVC.mockData = "data"
        default:
            print("Unknown destination type")
        }
    }
    
}

extension Data {
    
    func toKotlinByteArray() -> KotlinByteArray {
        let swiftByteArray = [UInt8](self)
        let intArray : [Int8] = swiftByteArray
            .map { Int8(bitPattern: $0) }
        
        let kotlinByteArray: KotlinByteArray = KotlinByteArray.init(size: Int32(swiftByteArray.count))
        for (index, element) in intArray.enumerated() {
            kotlinByteArray.set(index: Int32(index), value: element)
        }
        return kotlinByteArray
    }
    
}
