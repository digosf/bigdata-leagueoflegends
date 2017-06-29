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

        $output = shell_exec('/opt/spark-2.1.1-bin-hadoop2.7/bin/spark-submit /var/www/html/projeto-bigdata/project/loldatafinder_2.11-1.0.jar /var/www/html/projeto-bigdata/storage/app/' . $fileName . ' 2>&1');
        Storage::disk('local')->put($filename, $output);

        return json_encode(['data' => ['filename' => $fileName]]);
      }

    public function results ($id) {
       $apiLink = 'https://global.api.riotgames.com/api/lol/static-data/BR/v1.2/champion?champData=image&dataById=true&api_key=RGAPI-f08f49ff-27fb-4389-b413-fe17dc9d8370';
       $imageLink = 'http://ddragon.leagueoflegends.com/cdn/6.24.1/img/champion/';
       $champion_data = json_decode(file_get_contents($apiLink), true);

        $fileName = 'app/' . $id .'.json';
        $absolutePath = storage_path($fileName);
        $json_data = json_decode(file_get_contents($absolutePath), true);

        for( $i= 0; $i< count ($json_data[0]); $i ++) {
          $champions[$i]['name'] =  $champion_data['data'][$json_data[0][$i]]['name'];
          $champions[$i]['image'] = $imageLink . $champion_data['data'][$json_data[0][$i]]['image']['full'];
        }
        //dd($champions);

        for ($i = 1; $i < count($json_data); $i ++){
          for ($j = 0; $j < count($json_data[$i]); $j ++) {
            $content['adversary'][$j]['name'] = $champion_data['data'][$json_data[$i][$j][0]]['name'];
            $content['adversary'][$j]['image'] = $imageLink . $champion_data['data'][$json_data[$i][$j][0]]['image']['full'];
            $content['adversary'][$j]['value'] = number_format($json_data[$i][$j][1], 2, '.', ',');
            $content['adversary'][$j]['victories'] = $json_data[$i][$j][2];
            $content['adversary'][$j]['matches'] = $json_data[$i][$j][3];
          }
        }
        //dd($content);

        return view ('results', ['champions' => $champions,
                                 'content' => $content]);
    }
}
