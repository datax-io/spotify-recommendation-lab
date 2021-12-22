//
//  SettingsTableViewController.swift
//  App
//
//  Created by Alpha on 8/11/2021.
//  Copyright © 2021 Datax Limited. All rights reserved.
//

import UIKit

class SettingsTableViewController: UITableViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false

        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        print(indexPath.section)
        print(indexPath.row)

        if indexPath.section == 0 && indexPath.row == 0 {
            print("Change spotify")
            // do something
        }
    }


}
