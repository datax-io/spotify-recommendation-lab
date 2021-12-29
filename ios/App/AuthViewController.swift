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
    
    @IBOutlet weak var changeSpotifyClientIdButton: UIButton!
    @IBOutlet weak var currentSpotifyClientLabel: UILabel!
    @IBOutlet weak var getSpotifyAuthorizationButton: UIButton!
    @IBOutlet weak var currentSpotifyUserLabel: UILabel!
    
    @IBOutlet weak var fetchHistoryButton: UIButton!
    @IBOutlet weak var historyResultLabel: UILabel!
    @IBOutlet weak var fetchHistoryActivityIndicator: UIActivityIndicatorView!
    
    @IBOutlet weak var currentParcelAppLabel: UILabel!
    @IBOutlet weak var changeParcelAppIdButton: UIButton!
    @IBOutlet weak var currentParcelClientLabel: UILabel!
    @IBOutlet weak var changeParcelClientIdButton: UIButton!
    @IBOutlet weak var currentParcelUserLabel: UILabel!
    @IBOutlet weak var getParcelAuthorizationButton: UIButton!

    @IBOutlet weak var currentPygridHostLabel: UILabel!
    @IBOutlet weak var changePygridHostButton: UIButton!
    @IBOutlet weak var currentPygridTokenLabel: UILabel!
    @IBOutlet weak var changePygridTokenButton: UIButton!
    @IBOutlet weak var currentParticipantLabel: UILabel!
    
    @IBOutlet weak var changeParticipantIdButton: UIButton!
    @IBOutlet weak var startTrainingButton: UIButton!
    
    @IBOutlet weak var uploadDummyDocumentButton: UIButton!
    
    var token: String?
    static var tokenKey = "token"
        
    override func viewDidLoad() {
        super.viewDidLoad()
        workflowManager.callback = self
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
        let clientId = workflowManager.parcelHelper.clientId ?? ""
        guard let request = workflowManager.parcelHelper.getTokenRequest(clientId: clientId, authCode: fragment) else {
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
    
    func onParamsChanged() {
        self.currentParticipantLabel.text = "Participant ID: \(String(workflowManager.spotifyHistoryFetcher.participantId))"
        
        onSpotifyClientChanged(clientId: workflowManager.spotifyHelper.clientId)
        onSpotifyUserChanged(user: workflowManager.spotifyHelper.user)
        onParcelParamsChanged(clientId: workflowManager.parcelHelper.clientId, appId: workflowManager.parcelHelper.appId)
        onParcelUserChanged(user: workflowManager.parcelHelper.user)
        onPygridParamsChanged(host: workflowManager.pygridHelper.host, authToken: workflowManager.pygridHelper.authToken)
        onSpotifyHistoryStatusChanged(status: workflowManager.spotifyHistoryFetcher.getStatus())
        onTrainingReadinessChanged(ready: workflowManager.ready)
    }
    
    func onSpotifyClientChanged(clientId: String?) {
        guard let clientId = clientId else {
            self.currentSpotifyClientLabel.text = "Client ID not set"
            self.getSpotifyAuthorizationButton.isEnabled = false
            return
        }
        self.currentSpotifyClientLabel.text = "Client ID: \(clientId)"
        self.getSpotifyAuthorizationButton.isEnabled = true
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
    
    func onParcelParamsChanged(clientId: String?, appId: String?) {
        self.currentParcelClientLabel.text = clientId.map {"Client ID: \($0)"} ?? "Client ID not set"
        self.currentParcelAppLabel.text = appId.map {"App ID: \($0)"} ?? "App ID not set"
        guard let _ = clientId,
              let _ = appId else {
            self.getParcelAuthorizationButton.isEnabled = false
            return
        }
        self.getParcelAuthorizationButton.isEnabled = true
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
        self.historyResultLabel.text = "Fetching data..."
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

    func onPygridParamsChanged(host: String?, authToken: String?) {
        self.currentPygridHostLabel.text = host.map {"Host: \($0)"} ?? "Host not set"
        self.currentPygridTokenLabel.text = authToken != nil ? "Auth token set" : "Auth token not set"
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
    
    
    @IBAction func attemptChangeSpotifyClientId() {
        promptForInput(title: "Spotify Client ID", message: "Enter new Spotify client ID",
                       value: workflowManager.spotifyHelper.clientId ?? "") {
            workflowManager.spotifyHelper.clientId = $0
        }
    }
    
    @IBAction func attemptChangeParcelAppId() {
        promptForInput(title: "Parcel App ID", message: "Enter new Parcel app ID",
                       value: workflowManager.parcelHelper.appId ?? "") {
            workflowManager.parcelHelper.appId = $0
        }
    }
    
    @IBAction func attemptChangeParcelClientId() {
        promptForInput(title: "Parcel Client ID", message: "Enter new Parcel client ID",
                       value: workflowManager.parcelHelper.clientId ?? "") {
            workflowManager.parcelHelper.clientId = $0
        }
    }
    
    @IBAction func attemptChangePygridHost(_ sender: Any) {
        promptForInput(title: "PyGrid Host", message: "Enter new PyGrid host",
                       value: workflowManager.pygridHelper.host ?? "") {
            workflowManager.pygridHelper.host = $0
        }
    }
    
    @IBAction func attemptChangePygridToken(_ sender: Any) {
        promptForInput(title: "Spotify PyGrid token", message: "Enter new PyGrid token",
                       value: workflowManager.pygridHelper.authToken ?? "") {
            workflowManager.pygridHelper.authToken = $0
        }
    }
    
    @IBAction func attemptChangeParticipantId() {
        promptForInput(title: "Participant ID", message: "Enter new Participant ID",
                       value: String(workflowManager.spotifyHistoryFetcher.participantId)) {
            workflowManager.spotifyHistoryFetcher.participantId = Int32($0)!
        }
    }
    
    func promptForInput(title: String, message: String, value: String, valueHandler: @escaping ((String) -> Void)) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addTextField { $0.text = value }

        alert.addAction(UIAlertAction(title: "Save", style: .default, handler: { [weak alert] (_) in
            guard let textField = alert?.textFields?[0], let userText = textField.text else { return }
            valueHandler(userText)
        }))

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        self.present(alert, animated: true, completion: nil)
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
