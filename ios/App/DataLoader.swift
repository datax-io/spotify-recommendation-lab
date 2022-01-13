//
//  DataLoader.swift
//  Torch-Proto-Practice
//
//  MNISTLoader - Shao-Ping Lee on 3/26/16.
//  Reference From: https://github.com/simonlee2/MNISTKit
//  - Modified to add split loading of training and test data
//  - Added batching of data and labels

import Foundation
import SwiftSyft

// Load either train/test data
// Batch data according to batch size

enum DataSetType {
    case train
    case test
}

extension NSData {
    func bigEndianInt32(location: Int) -> UInt32? {
        var value: UInt32 = 0
        self.getBytes(&value, range: NSRange(location: location, length: MemoryLayout<UInt32>.size))
        return UInt32(bigEndian: value)
    }

    func bigEndianInt32s(range: Range<Int>) -> [UInt32] {
        return range.compactMap({bigEndianInt32(location: $0 * MemoryLayout<UInt32>.size)})
    }
}

// Lazy chunked array
// https://forums.swift.org/t/chunking-collections-and-strings-in-swift-5-1/26524/8

public struct LazyChunkSequence<T: Collection>: Sequence, IteratorProtocol {

    private var baseIterator: T.Iterator
    private let size: Int

    fileprivate init(over collection: T, chunkSize size: Int) {
        baseIterator = collection.lazy.makeIterator()
        self.size = size
    }

    mutating public func next() -> [T.Element]? {
        var chunk: [T.Element] = []

        var remaining = size
        while remaining > 0, let nextElement = baseIterator.next() {
            chunk.append(nextElement)
            remaining -= 1
        }

        if chunk.count < size {
            return nil
        }

        return chunk.isEmpty ? nil : chunk
    }

}

extension Collection {

    public func lazyChunkSequence(size: Int) -> LazyChunkSequence<Self> {
        return LazyChunkSequence(over: self, chunkSize: size)
    }

}
