<?php
require_once __DIR__ . '/config.php';

$token = $_GET['token'] ?? '';
if ($token !== 'korentech5kk') {
    http_response_code(403);
    die('Forbidden');
}

header('Content-Type: text/plain; charset=utf-8');

try {
    $db = getDB();
    
    // List characters
    $stmt = $db->query("SELECT charId, char_name, account_name, online FROM characters");
    $chars = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "Found " . count($chars) . " characters:\n";
    
    foreach ($chars as $c) {
        $charId = (int)$c['charId'];
        $charName = $c['char_name'];
        echo "- Char: {$charName} (ID: {$charId}, Account: {$c['account_name']}, Online: {$c['online']})\n";
        
        // Check if character already has Adena (item_id 57) in INVENTORY
        $checkStmt = $db->prepare("SELECT object_id, count FROM items WHERE owner_id = ? AND item_id = 57 AND loc = 'INVENTORY'");
        $checkStmt->execute([$charId]);
        $adenaItem = $checkStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($adenaItem) {
            $newCount = (int)$adenaItem['count'] + 5000000;
            $updStmt = $db->prepare("UPDATE items SET count = ? WHERE object_id = ?");
            $updStmt->execute([$newCount, $adenaItem['object_id']]);
            echo "  Updated existing Adena to {$newCount} (added 5kk)\n";
        } else {
            // Need a new object_id
            $maxStmt = $db->query("SELECT MAX(object_id) as max_id FROM items");
            $maxRow = $maxStmt->fetch(PDO::FETCH_ASSOC);
            $newObjId = max(268435456, (int)($maxRow['max_id'] ?? 0) + 1);
            
            $insStmt = $db->prepare("INSERT INTO items (owner_id, object_id, item_id, count, enchant_level, loc, loc_data, time_of_use, custom_type1, custom_type2, mana_left, time) VALUES (?, ?, 57, 5000000, 0, 'INVENTORY', 0, 0, 0, 0, -1, 0)");
            $insStmt->execute([$charId, $newObjId]);
            echo "  Inserted new Adena item with 5,000,000 Adena (Object ID: {$newObjId})\n";
        }
    }
    
    echo "\nSuccess! All existing characters received 5kk Adena.\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
