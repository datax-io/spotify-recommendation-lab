//
//  TrainingViewController.swift
//  App
//
//  Created by Alpha on 22/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import UIKit
import shared
import SwiftSyft
import AppAuth


class TrainingViewController: UIViewController {

    private var syftJob: SyftJob?
    private var syftClient: SyftClient?

    @IBOutlet weak var trainingResultLabel: UILabel!

    @IBOutlet weak var stopButton: UIBarButtonItem!
    
    var data: [TrackTrainingData] = []

    let pygridHost = workflowManager.pygridHelper.host
    let pygridAuthToken = workflowManager.pygridHelper.authToken

    let modelName = workflowManager.pygridHelper.modelName
    let modelVersion = workflowManager.pygridHelper.modelVersion

    var sourceParticipantId = Int(workflowManager.pygridHelper.participantId)
    let participantId = Int(workflowManager.pygridHelper.participantId) // 1-based
    let numOfParticipants = Int(workflowManager.pygridHelper.numOfParticipants)

    override func viewDidLoad() {
        super.viewDidLoad()
        self.trainingResultLabel.text = ""

        appendText(sourceParticipantId == 0 ? "Data source: API" : "Data source: CSV")
        appendText("Participant ID: \(participantId)")

        data = workflowManager.spotifyHistoryFetcher.trainingData(participantId: Int32(sourceParticipantId), normalizeScores: true)

        appendText("Data: \(data.count) tracks")

        guard data.count > 0 else {
            appendText("No data")
            return
        }

        self.startTraining()
    }

    func startTraining() {
        guard let host = pygridHost else {
            print("PyGrid host is not set")
            return
        }
        guard let syftClient = SyftClient(url: URL(string: host)!, authToken: pygridAuthToken) else {
            appendText("No syft client")
            return
        }

        guard let featureAndLabelTensors = try? MovieLensLoader.loadAsTensors(data: self.data, setType: .train) else {
            return
        }

        appendText("Model: \(modelName)")
        appendText("Version: \(modelVersion)")

        self.syftClient = syftClient
        appendText("Connecting to \(host)")

        let job = syftClient.newJob(modelName: modelName, version: modelVersion)
        self.syftJob = job

        job.onReady(execute: { modelParams, plans, clientConfig, modelReport in
            let batchSize = clientConfig.batchSize

            DispatchQueue.main.sync {
                self.appendText("Loading data, batch size = \(batchSize)")
            }

            let dataLoader = MultiTensorDataLoader(dataset: featureAndLabelTensors, shuffle: true, batchSize: batchSize)

            DispatchQueue.main.sync {
                self.appendText("Training...")
            }

            for batchedTensors in dataLoader {

                autoreleasepool {

                    let featureTensors = batchedTensors[0]
                    let labelTensors = batchedTensors[1]

                    let learningRateArray = [clientConfig.learningRate]

                    var userRow = [UInt8](repeating: 0, count: self.numOfParticipants)
                    userRow[self.participantId - 1] = 1

                    let userArray = [[UInt8]](repeating: userRow, count: batchSize).flatMap { $0 }

                    let xsArray = [[Float]](repeating: [Float](repeating: 0.0, count: MovieLensLoader.embeddingOutputSize + MovieLensLoader.featureSize), count: batchSize).flatMap { $0 }

                    guard
                        let userTensors = TorchTensor.new(array: userArray, size: [batchSize, self.numOfParticipants]),
                        let xTensors = TorchTensor.new(array: xsArray, size: [batchSize, xsArray.count / batchSize]),
                        let learningRateTensor = TorchTensor.new(array: learningRateArray, size: [1]) ,
                        let modelParamTensors = modelParams.paramTensorsForTraining else
                    {
                        return
                    }

                    let result = plans["training_plan"]?.forward([TorchIValue.new(with: featureTensors),
                                                                  TorchIValue.new(with: userTensors),
                                                                  TorchIValue.new(with: xTensors),
                                                                  TorchIValue.new(with: labelTensors),
                                                                  TorchIValue.new(with: learningRateTensor),
                                                                  TorchIValue.new(withTensorList: modelParamTensors)
                                                                 ])

                    guard let tensorResults = result?.toTensorList() else {
                        return
                    }

                    let lossTensor = tensorResults[0]
                    lossTensor.print()

                    modelParams.paramTensorsForTraining = Array(tensorResults.dropFirst())

                }
            }

            // Generate diff data (subtract original model params from updated params) and report the final diffs as
            guard let diffStateData = modelParams.generateDiffData() else {
                DispatchQueue.main.sync {
                    self.appendText("Failed to generate diff")
                }
                return
            }

            DispatchQueue.main.sync {
                self.appendText("Uploading diff...")
                
                workflowManager.parcelHelper.uploadDocument(data: NSData(data: diffStateData), fileName: "diff.dat") { document, error in
                    guard let documentId = document?.id else {
                        print("Error uploading document")
                        self.appendText("Error uploading document")
                        return
                    }
                    print("Diff document ID: \(documentId)")
                    self.appendText("Uploaded document \(documentId)")
                    self.appendText("Reporting diff...")
                    job.reportParcelDiff(diffDocumentId: documentId)
                    self.appendText("Done")
                }
            }

        })

        job.onRejected(execute: { timeout in
            DispatchQueue.main.sync {
                self.appendText("Rejected")
            }
        })

        job.onError(execute: { error in
            print(error)
            DispatchQueue.main.sync {
                self.appendText(error.localizedDescription)
            }
        })

        job.start(chargeDetection: false, wifiDetection: false)

    }

    @IBAction func stopTraining(_ sender: Any) {
        self.navigationController?.popViewController(animated: true)
    }

    func appendText(_ text: String){
        guard let currentText = self.trainingResultLabel.text else {
            self.trainingResultLabel.text = text
            return
        }
        self.trainingResultLabel.text = "\(currentText)\n\(text)"
    }

}
