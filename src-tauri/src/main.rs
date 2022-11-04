use tauri::Manager;
use std::process::Command;
use std::str;

#[cfg_attr(
  all(not(debug_assertions), target_os = "windows"),
  windows_subsystem = "windows"
)]

// #[derive(Clone, serde::Serialize)]
// struct Payload {
//     message: String,
// }

#[tauri::command]
fn custom_command(value: String) -> String{
    let result = Command::new("net")
        .args(&["use", "z:", "\\\\192.168.1.5\\PublicData\\VueronDataSet\\3_other_data"])
        .output()
        .expect("filed to execute");

    println!("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    let ret = match str::from_utf8(&result.stdout) {
        Ok(v) => v,
        Err(e) => panic!("invalid utf-8 seq: {}", e),
    };

    // println!("resutl: {}", s.to_string());
    // s.to_string()
    // (&"?????????????????????").to_string()
    // let s = result.stdout.to_string();
    ret.to_string()
}

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![custom_command])
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
