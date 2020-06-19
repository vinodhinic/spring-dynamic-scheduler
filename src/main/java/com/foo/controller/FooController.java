package com.foo.controller;

import com.foo.config.FooSync;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
@RequestMapping("/foo-sync")
public class FooController {

    @Autowired
    private FooSync fooSync;

    @RequestMapping(value = "/updateSyncSchedule", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void updateSyncSchedule(
            @RequestParam("scheduleInSeconds") Integer scheduleInSeconds) {
        fooSync.updateSchedule(scheduleInSeconds);
    }

    @RequestMapping(value = "/nextSyncSchedule", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Date getNextSchedule() {
        return fooSync.getNextSyncExecutionTime();
    }

    @ApiOperation(
            "Triggers the next instance of foo sync. Useful when the next run is an hour away and you don't want to wait until then")
    @RequestMapping(value = "/triggerSync", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void trigger(HttpServletRequest httpServletRequest) {
        fooSync.trigger();
    }
}
