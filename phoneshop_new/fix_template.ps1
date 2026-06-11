$file = 'backend\src\main\resources\templates\product-detail.html'
$content = Get-Content $file -Raw

$oldText = 'th:text="${(v.storageGb != null ? v.storageGb + ' + "'GB'" + ' : ' + "''" + ') + (v.color != null ? ' + "' - '" + ' : ' + "''" + ') + (v.color ?: ' + "''" + ')}">'
$newText = 'th:text="${(v.storageGb != null ? v.storageGb + ' + "'GB'" + ' : ' + "''" + ') + (#strings.hasText(v.color) ? ' + "' - '" + ' + v.color : ' + "''" + ')}">'

$content = $content.Replace($oldText, $newText)
Set-Content $file $content -NoNewline
Write-Host "Fixed successfully"
