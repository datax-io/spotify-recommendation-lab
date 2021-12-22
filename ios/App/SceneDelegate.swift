//
//  SceneDelegate.swift
//  spotify-recommendation-lab
//
//  Created by Alpha on 3/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let windowScene = scene as? UIWindowScene {
         let window = UIWindow(windowScene: windowScene)


            let storyboard = UIStoryboard(name: "Main", bundle: nil)

            let initialViewController = storyboard.instantiateInitialViewController()

          window.rootViewController = initialViewController

          self.window = window
          window.makeKeyAndVisible()
        }
    }
    
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else {
            print("no url")
            return
        }
        guard let host = url.host else {
            print(url)
            return
        }
        guard let fragment = url.fragment else {
            print(url)
            return
        }
        
        let topViewController = self.window?.rootViewController as? UINavigationController
        
        if (topViewController?.topViewController as? AuthViewController == nil) {
            topViewController?.popToRootViewController(animated: true)
        }
        
        guard let currentVC = topViewController?.topViewController as? AuthViewController else {
            return
        }
        
        print(host)
        
        switch host {
        case "spotifyauth":
            currentVC.handleSpotifyAuthorization(fragment)
        case "parcelauth":
            currentVC.handleParcelAuthorization(fragment)
        default:
            print(url.host)
        }
    }


    func sceneDidDisconnect(_ scene: UIScene) {
        // Called as the scene is being released by the system.
        // This occurs shortly after the scene enters the background, or when its session is discarded.
        // Release any resources associated with this scene that can be re-created the next time the scene connects.
        // The scene may re-connect later, as its session was not necessarily discarded (see `application:didDiscardSceneSessions` instead).
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        // Called when the scene has moved from an inactive state to an active state.
        // Use this method to restart any tasks that were paused (or not yet started) when the scene was inactive.
    }

    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from an active state to an inactive state.
        // This may occur due to temporary interruptions (ex. an incoming phone call).
    }

    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from the background to the foreground.
        // Use this method to undo the changes made on entering the background.
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from the foreground to the background.
        // Use this method to save data, release shared resources, and store enough scene-specific state information
        // to restore the scene back to its current state.
    }


}

