<?php
/**
 * Infolot TV App - Proxy WebService
 * Alojado en: app.gesloto.es/proxy.php
 * Reenvía peticiones al WebService evitando restricciones CORS del navegador.
 */

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    exit;
}

$body = file_get_contents('php://input');
if (empty($body)) {
    http_response_code(400);
    echo json_encode(['error' => 'Empty request body']);
    exit;
}

$data = json_decode($body, true);
if (!$data || !isset($data['_endpoint'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing _endpoint parameter']);
    exit;
}

$endpoint = preg_replace('/[^a-zA-Z0-9\-]/', '', $data['_endpoint']);
$host     = (isset($data['_target']) && $data['_target'] === 'dev')
    ? 'dev-webservice.infolot.es'
    : 'webservice.infolot.es';
$targetUrl = 'https://' . $host . '/ws/' . $endpoint;

unset($data['_endpoint']);
unset($data['_target']);
$forwardBody = json_encode($data);

$ch = curl_init($targetUrl);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => $forwardBody,
    CURLOPT_HTTPHEADER     => ['Content-Type: application/json'],
    CURLOPT_TIMEOUT        => 15,
    CURLOPT_SSL_VERIFYPEER => true,
]);

$response  = curl_exec($ch);
$httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError = curl_error($ch);
curl_close($ch);

if ($curlError) {
    http_response_code(502);
    echo json_encode(['error' => 'Proxy error: ' . $curlError]);
    exit;
}

http_response_code($httpCode);
echo $response;
