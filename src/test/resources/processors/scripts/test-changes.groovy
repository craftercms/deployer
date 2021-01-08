import org.craftercms.deployer.api.ChangeSet

def testService = applicationContext.getBean("testService")

logger.info("Found external service {}", testService)

logger.info("Updating change set")

// Return a new change set
return new ChangeSet(filteredChangeSet.createdFiles, [ testService.updatedFile ], [])
