import Foundation
import SwiftSyft
import shared

enum MovieLensError: Error {
    case fileError
    case tensorConversionError
}

extension Collection where Iterator.Element : TrackTrainingData {
    func toTracksAndLabels() -> (tracks: [[Float]], labels: [Float]) {
        return (
            tracks: self.map { $0.trainingDataRow.map { $0.floatValue } },
            labels: self.map { $0.normalizedScore?.floatValue ?? $0.score }
        )
    }
}

class MovieLensLoader {
    
    static let featureSize = 10
    static let embeddingOutputSize = 50
    
    static func loadAsTensors(data: [TrackTrainingData], setType: DataSetType) throws -> [[TorchTensor]] {
        let (tracks, labels) = data.toTracksAndLabels()
        switch setType {
        case .train:
            return try convertToTensors(trackArrays: tracks, labels: labels)
        case .test:
            return try convertToTensors(trackArrays: tracks, labels: labels)
        }
    }
    
    static func convertToTensors(trackArrays: [[Float]], labels: [Float]) throws -> [[TorchTensor]] {
        var resultTensors: [[TorchTensor]] = []
        for (trackArray, label) in zip(trackArrays, labels) {
            guard let trackTensor = TorchTensor.new(array: trackArray, size: [1, featureSize]) else {
                throw MovieLensError.tensorConversionError
            }
            
            let labelArray = [UInt8](repeating: UInt8(max(0, label)), count: 1)
            guard let labelTensor = TorchTensor.new(array: labelArray, size: [1, 1]) else {
                throw MovieLensError.tensorConversionError
            }
            
            resultTensors.append([trackTensor, labelTensor])
        }
        return resultTensors
    }

}
