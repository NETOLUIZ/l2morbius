<?php
require_once __DIR__ . '/config.php';

$token = $_GET['token'] ?? '';
if ($token !== 'korentech5kk') {
    http_response_code(403);
    die('Forbidden');
}

header('Content-Type: text/plain; charset=utf-8');

if (!$mysqli) {
    die("Database not connected\n");
}

$res = mysqli_query($mysqli, "SELECT charId, char_name, account_name, online FROM characters");
if (!$res) {
    die("Error querying characters: " . mysqli_error($mysqli) . "\n");
}

echo "Found " . mysqli_num_rows($res) . " characters:\n";

while ($c = mysqli_fetch_assoc($res)) {
    $charId = (int)$c['charId'];
    $charName = $c['char_name'];
    echo "- Char: {$charName} (ID: {$charId}, Account: {$c['account_name']}, Online: {$c['online']})\n";
    
    // Check if character already has Adena (item_id 57) in INVENTORY
    $chk = mysqli_query($mysqli, "SELECT object_id, count FROM items WHERE owner_id = {$charId} AND item_id = 57 AND loc = 'INVENTORY'");
    $adenaItem = mysqli_fetch_assoc($chk);
    
    if ($adenaItem) {
        $newCount = (int)$adenaItem['count'] + 5000000;
        mysqli_query($mysqli, "UPDATE items SET count = {$newCount} WHERE object_id = {$adenaItem['object_id']}");
        echo "  Updated existing Adena to {$newCount} (added 5kk)\n";
    } else {
        // Need a new object_id
        $maxRes = mysqli_query($mysqli, "SELECT MAX(object_id) as max_id FROM items");
        $maxRow = mysqli_fetch_assoc($maxRes);
        $newObjId = max(268435456, (int)($maxRow['max_id'] ?? 0) + 1);
        
        $sql = "INSERT INTO items (owner_id, object_id, item_id, count, enchant_level, loc, loc_data, time_of_use, custom_type1, custom_type2, mana_left, time) VALUES ({$charId}, {$newObjId}, 57, 5000000, 0, 'INVENTORY', 0, 0, 0, 0, -1, 0)";
        mysqli_query($mysqli, $sql);
        echo "  Inserted new Adena item with 5,000,000 Adena (Object ID: {$newObjId})\n";
    }
}

echo "\nSuccess! All existing characters received 5kk Adena.\n";
