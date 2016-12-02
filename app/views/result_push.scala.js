@(id: String, chartEndpoint: String)(implicit r: RequestHeader)

var rttArr = []
var pktReq = 0

var resultTable = $("#result-table").DataTable({
//  "dom": 'T<"clear">lfrtip',
  "dom": "<'row'<'col-sm-6'lf><'col-sm-6'<'pull-right'B>>>rtip",
  "bFilter": false,
  "buttons": ['copy', 'excel', 'csv', 'pdf']
})

function setProgress(percent) {
  percent = Math.floor(percent)
  var progress = $("#progress")
  if(percent == 0 || percent == 100){
    progress.removeClass("active")
  }else if(percent > 0 && percent < 100){
    if(!progress.hasClass("active")) progress.addClass("active")
  }
  progress.css("width", percent + "%").attr("aria-valuenow", percent)
}

function newBenchmark(n) {
  rttArr = []
  pktReq = n
  setProgress(0)
  $("#result-alert").hide()
  resultTable.clear().draw()
}

function addBenchmarkResult(msg) {

  var rtt = msg.totalTime
  
  var responseMessage = msg.message
  if(responseMessage.length > 32) {
    responseMessage = responseMessage.substr(0, 32) + "... [length = " + (responseMessage.length) + " chars]"
  }
  var pt = msg.processingTime
  var diff = rtt - pt
  var diffPercent = ((diff/rtt)*100).toFixed(2)
  var row = [msg.targetAddress, msg.success, escapeHTML(responseMessage), rtt, pt, diff]
  resultTable.row.add(row).draw(false)

  rttArr.push(rtt)
  
  setProgress(Math.floor(rttArr.length * 100/pktReq))
  
  if(rttArr.length == pktReq){
    var sumRtt = 0
    for(i in rttArr){
      sumRtt += rttArr[i]
    }
    var avgRtt = sumRtt/rttArr.length
    var sumDev = 0
    for(i in rttArr){
      sumDev += Math.pow((rttArr[i] - avgRtt), 2) 
    }
    var stdDev = Math.sqrt(sumDev/rttArr.length)
    $("#result-alert").fadeIn()
    var summary = "Avg = " + avgRtt.toFixed(2) + " ms, Std.Dev. = " + stdDev.toFixed(2) + " ms"
    $("#result-summary").text(summary)
  }
}

var clientId = localStorage.getItem("clientId")
if(!clientId) {
    clientId = Math.random().toString(36).substring(2)
    localStorage.setItem("clientId", clientId)
}
console.log(clientId)
var WS = window["MozWebSocket"] ? MozWebSocket : WebSocket
var chartSocket = new WS("@chartEndpoint")
chartSocket.onmessage = function(e) {
    var msg = JSON.parse(e.data)
  addBenchmarkResult(msg)
}