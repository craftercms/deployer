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
              "text": "A deployment in server ${serverName} has exceeded the configured threshold of ${target.getRuntimeWarningThreshold()} seconds:\n"
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
                  "text": ": ${target.id}"
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
                  "text": ": ${payload.getStart().format(dateTimeFormatter)}"
                }
              ]
            },
            {
              "type": "rich_text_section",
              "elements": [
                {
                  "type": "text",
                  "text": "Current runtime",
                  "style": {
                    "bold": true
                  }
                },
                {
                  "type": "text",
                  "text": ": ${durationFormatter.apply(event.getRuntime())}"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
  <#if payloadJson??>,
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
