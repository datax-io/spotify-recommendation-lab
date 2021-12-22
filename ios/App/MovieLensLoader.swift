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
            tracks: self.map { [
                Float($0.user),
                $0.acousticness,
                $0.danceability,
                $0.durationMs,
                $0.energy,
                $0.instrumentalness,
                $0.liveness,
                $0.loudness,
                $0.speechiness,
                $0.tempo,
                $0.valence,
            ] },
            labels: self.map { $0.score }
        )
    }
}

class MovieLensLoader {
    
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
            guard let trackTensor = TorchTensor.new(array: trackArray, size: [1, 11]) else {
                throw MovieLensError.tensorConversionError
            }
            
            var oneHotRow = [UInt8](repeating: 0, count: 1)
            oneHotRow[0] = UInt8(label)
            
            guard let labelTensor = TorchTensor.new(array: oneHotRow, size: [1, 1]) else {
                throw MovieLensError.tensorConversionError
            }
            
            resultTensors.append([trackTensor, labelTensor])
        }
        return resultTensors
    }
    
    static func load(data: [TrackTrainingData], setType: DataSetType, batchSize: Int) throws -> (data: LazyChunkSequence<[[Float]]>, labels: LazyChunkSequence<[Float]>) {
        let (tracks, labels) = data.toTracksAndLabels()
        switch setType {
        case .train:
            return (tracks.lazyChunkSequence(size: batchSize), labels.lazyChunkSequence(size: batchSize))
        case .test:
            return (tracks.lazyChunkSequence(size: batchSize), labels.lazyChunkSequence(size: batchSize))
        }
        
    }

}
