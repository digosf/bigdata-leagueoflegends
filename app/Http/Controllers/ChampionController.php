<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

class ChampionController extends Controller
{
    public function index() {
      return view('index');
    }

    public function store(Request $request) {
        $champions = $request->get('champions');
        $fileName = 'champions_' . uniqidReal() . '.json';
        $absolutePath = storage_path($fileName);

        Storage::disk('local')->put($fileName, json_encode($champions));

        $output = shell_exec('/opt/spark-2.1.1-bin-hadoop2.7/bin/spark-submit /var/www/html/projeto-bigdata/project/lolapp_2.11-1.0.jar /var/www/html/projeto-bigdata/storage/app/' . $fileName . ' 2>&1');
        Storage::disk('local')->put('output.txt', $output);
		//Storage::disk('local')->delete($fileName);

        return json_encode(['data' => ['success' => true]]);
      }
      public function results () {
        $content = Storage::disk('local')->get('results.txt');
        return view ('results', ['content' => $content]);
      }
}
