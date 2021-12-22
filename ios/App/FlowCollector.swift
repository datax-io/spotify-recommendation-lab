//
//  FlowCollector.swift
//  App
//
//  Created by Alpha on 23/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import Foundation
import shared

class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    
    var handler: (T?) -> Void
    
    init(_ fn: @escaping (T?) -> Void){
        self.handler = fn
    }
    
    func emit(value: Any?, completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        guard let value = value as? T else {
            print("Incorrect type")
            completionHandler(KotlinUnit(), nil)
            return
        }
        handler(value)
        print("ios received")
        completionHandler(KotlinUnit(), nil)
    }
}
