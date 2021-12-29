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

    var mockData: String?

    private var syftJob: SyftJob?
    private var syftClient: SyftClient?

    @IBOutlet weak var trainingResultLabel: UILabel!

    @IBOutlet weak var stopButton: UIBarButtonItem!
    
    let data = workflowManager.spotifyHistoryFetcher.trainingData()

    let pygridHost = workflowManager.pygridHelper.host

    let pygridAuthToken = workflowManager.pygridHelper.authToken

    override func viewDidLoad() {
        super.viewDidLoad()
        print(self.mockData)
        
        self.trainingResultLabel.text = ""
        appendText("Data: \(data.count)")

        self.startTrainingMNIST()
        
    }

    func startTrainingMNIST() {
        guard let host = pygridHost else {
            print("PyGrid host is not set")
            return
        }
        guard let syftClient = SyftClient(url: URL(string: host)!, authToken: pygridAuthToken) else {
            appendText("No syft client")
            return
        }

        self.syftClient = syftClient
        appendText("Connecting to \(host)")

        self.syftJob = syftClient.newJob(modelName: "mnist", version: "1.0")

        self.syftJob?.onReady(execute: { modelParams, plans, clientConfig, modelReport in
            DispatchQueue.main.sync {
                self.appendText("Loading data")
            }

            // This returns an array for each MNIST image and the corresponding label as PyTorch tensor
            // It divides the training data and the label by batches
            guard let MNISTDataAndLabelTensors = try? MNISTLoader.loadAsTensors(setType: .train) else {
                return
            }

            // This loads the MNIST tensor into a dataloader to use for iterating during training
            let dataLoader = MultiTensorDataLoader(dataset: MNISTDataAndLabelTensors, shuffle: true, batchSize: 64)
            
            DispatchQueue.main.sync {
                self.appendText("Training")
            }

            // Iterate through each batch of MNIST data and label
            for batchedTensors in dataLoader {

                // We need to create an autorelease pool to release the training data from memory after each loop
                autoreleasepool {

                    // Preprocess MNIST data by flattening all of the MNIST batch data as a single array
                    let MNISTTensors = batchedTensors[0].reshape([-1, 784])

                    // Preprocess the label ( 0 to 9 ) by creating one-hot features and then flattening the entire thing
                    let labels = batchedTensors[1]

                    // Add batch_size, learning_rate and model_params as tensors
                    let batchSize = [UInt32(clientConfig.batchSize)]
                    let learningRate = [clientConfig.learningRate]

                    guard
                        let batchSizeTensor = TorchTensor.new(array: batchSize, size: [1]),
                        let learningRateTensor = TorchTensor.new(array: learningRate, size: [1]) ,
                        let modelParamTensors = modelParams.paramTensorsForTraining else
                    {
                        return
                    }

                    // Execute the torchscript plan with the training data, validation data, batch size, learning rate and model params
                    let result = plans["training_plan"]?.forward([TorchIValue.new(with: MNISTTensors),
                                                                  TorchIValue.new(with: labels),
                                                                  TorchIValue.new(with: batchSizeTensor),
                                                                  TorchIValue.new(with: learningRateTensor),
                                                                  TorchIValue.new(withTensorList: modelParamTensors)])

                    // Example returns a list of tensors in the folowing order: loss, accuracy, model param 1,
                    // model param 2, model param 3, model param 4
                    guard let tensorResults = result?.toTensorList() else {
                        return
                    }

                    let lossTensor = tensorResults[0]
                    lossTensor.print()
                    let loss = lossTensor.item()

                    let accuracyTensor = tensorResults[1]
                    accuracyTensor.print()

                    // Get updated param tensors and update them in param tensors holder
                    let param1 = tensorResults[2]
                    let param2 = tensorResults[3]
                    let param3 = tensorResults[4]
                    let param4 = tensorResults[5]

                    modelParams.paramTensorsForTraining = [param1, param2, param3, param4]

                }
            }

            // Generate diff data (subtract original model params from updated params) and report the final diffs as
            guard let diffStateData = modelParams.generateDiffData() else {
                return
            }
            
            DispatchQueue.main.sync {
                self.appendText("Reporting diff")
            }

            // Submit model params diff to server
            modelReport(diffStateData)
            
            // TODO upload via AF
            DispatchQueue.main.sync {
                self.appendText("Done")
            }

        })

        self.syftJob?.onRejected(execute: { timeout in
            DispatchQueue.main.sync {
                self.appendText("Rejected")
            }
        })

        self.syftJob?.onError(execute: { error in
            print(error)
            DispatchQueue.main.sync {
                self.appendText(error.localizedDescription)
            }
        })

        self.syftJob?.start(chargeDetection: false, wifiDetection: false)

    }

    func startTraining() {
        
        guard let MovieLensDataAndLabelTensors = try? MovieLensLoader.loadAsTensors(data: self.data, setType: .train) else {
            return
        }
        
        guard let host = pygridHost else {
            print("PyGrid host is not set")
            return
        }
        guard let syftClient = SyftClient(url: URL(string: host)!, authToken: pygridAuthToken) else {
            appendText("No syft client")
            return
        }

        self.syftClient = syftClient
        appendText("Connecting to \(host)")

        self.syftJob = syftClient.newJob(modelName: "mnist", version: "1.0") // TODO model name

        self.syftJob?.onReady(execute: { modelParams, plans, clientConfig, modelReport in
            DispatchQueue.main.sync {
                self.appendText("Loading data")
            }

            let dataLoader = MultiTensorDataLoader(dataset: MovieLensDataAndLabelTensors, shuffle: true, batchSize: 64)

            DispatchQueue.main.sync {
                self.appendText("Training")
            }

            for batchedTensors in dataLoader {

                autoreleasepool {

                    let trackTensors = batchedTensors[0]
                    let labelTensors = batchedTensors[1]

                    // Add batch_size, learning_rate and model_params as tensors
                    let batchSize = [UInt32(clientConfig.batchSize)]
                    let learningRate = [clientConfig.learningRate]

                    guard
                        let batchSizeTensor = TorchTensor.new(array: batchSize, size: [1]),
                        let learningRateTensor = TorchTensor.new(array: learningRate, size: [1]) ,
                        let modelParamTensors = modelParams.paramTensorsForTraining else
                    {
                        return
                    }

                    let result = plans["training_plan"]?.forward([TorchIValue.new(with: trackTensors),
                                                                  TorchIValue.new(with: labelTensors),
                                                                  TorchIValue.new(with: batchSizeTensor),
                                                                  TorchIValue.new(with: learningRateTensor),
                                                                  TorchIValue.new(withTensorList: modelParamTensors)])

                    // model param 2, model param 3, model param 4
                    guard let tensorResults = result?.toTensorList() else {
                        return
                    }

                    let lossTensor = tensorResults[0]
                    lossTensor.print()
                    let loss = lossTensor.item()

                    let accuracyTensor = tensorResults[1]
                    accuracyTensor.print()

                    let param1 = tensorResults[2]
                    let param2 = tensorResults[3]
                    let param3 = tensorResults[4]
                    let param4 = tensorResults[5]

                    modelParams.paramTensorsForTraining = [param1, param2, param3, param4]

                }
            }

            // Generate diff data (subtract original model params from updated params) and report the final diffs as
            guard let diffStateData = modelParams.generateDiffData() else {
                return
            }

            DispatchQueue.main.sync {
                self.appendText("Reporting diff")
            }

            // Submit model params diff to server
            modelReport(diffStateData)

            // TODO upload via AF
            DispatchQueue.main.sync {
                self.appendText("Done")
            }

        })

        self.syftJob?.onRejected(execute: { timeout in
            DispatchQueue.main.sync {
                self.appendText("Rejected")
            }
        })

        self.syftJob?.onError(execute: { error in
            print(error)
            DispatchQueue.main.sync {
                self.appendText(error.localizedDescription)
            }
        })

        self.syftJob?.start(chargeDetection: false, wifiDetection: false)

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
