{
"blocks": [
{
"type": "rich_text",
"elements": [
{
"type": "rich_text_section",
"elements": [
{
"type": "text",
"text": "A deployment in server ${serverName} has just finished:\n"
}
]
},
{
"type": "rich_text_list",
"style": "bullet",
"indent": 0,
"border": 1,
"elements": [
{
"type": "rich_text_section",
"elements": [
{
"type": "text",
"text": "Target ID",
"style": {
"bold": true
}
},
{
"type": "text",
"text": ": ${targetId}"
}
]
},
{
"type": "rich_text_section",
"elements": [
{
"type": "text",
"text": "Start",
"style": {
"bold": true
}
},
{
"type": "text",
"text": ": ${start}"
}
]
},
{
"type": "rich_text_section",
"elements": [
{
"type": "text",
"text": "End",
"style": {
"bold": true
}
},
{
"type": "text",
"text": ": ${end}"
}
]
},
{
"type": "rich_text_section",
"elements": [
{
"type": "text",
"text": "Status",
"style": {
"bold": true
}
},
{
"type": "text",
"text": ": ${status}"
}
]
}
]
}
]
}
]
<#if deploymentJson??>
	,
	"attachments": [
	{
	"blocks": [
	{
	"type": "section",
	"text": {
	"type": "mrkdwn",
	"text": "*Deployment output:*"
	}
	},
	{
	"type": "section",
	"text": {
	"type": "plain_text",
	"text": "${payloadJson?json_string}"
	}
	}
	]
	}
	]
</#if>
}
