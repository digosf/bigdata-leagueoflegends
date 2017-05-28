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

        exec('sleep 30');

        Storage::disk('local')->delete($fileName);

        return json_encode(['data' => ['success' => true]]);
    }
}
